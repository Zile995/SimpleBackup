package com.stefan.simplebackup.backup

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.databinding.ActivityBackupBinding
import com.stefan.simplebackup.utils.FileUtil
import com.stefan.simplebackup.utils.SuperUser
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.*
import kotlin.math.pow


class BackupActivity : AppCompatActivity() {

    companion object {
        private const val ROOT: String = "SimpleBackup/local"
        private const val TAG: String = "BackupActivity"
        private const val REQUEST_CODE_SIGN_IN: Int = 400
    }

    private var internalStoragePath: String = ""
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var toolBar: Toolbar
    private lateinit var textItem: MaterialTextView
    private lateinit var appImage: ImageView
    private lateinit var apkSize: Chip
    private lateinit var dataSize: Chip
    private lateinit var backupButton: MaterialButton
    private lateinit var backupDriveButton: MaterialButton
    private lateinit var chipVersion: Chip
    private lateinit var chipPackage: Chip
    private lateinit var chipDir: Chip
    private lateinit var deleteButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        val binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind Views
        bindViews(binding)
        setToolBar()

        val selectedApp: Application? = intent?.extras?.getParcelable("application")
        val bitmap = BitmapFactory.decodeStream(this.openFileInput(selectedApp?.getName()))

        textItem.text = selectedApp?.getName()
        chipPackage.text = (selectedApp?.getPackageName() as CharSequence).toString()
        chipVersion.text = (selectedApp.getVersionName() as CharSequence).toString()
        chipDir.text = (selectedApp.getDataDir() as CharSequence).toString()
        apkSize.text = this.getString(R.string.apk_size, selectedApp.getApkSize())
        dataSize.text = this.getString(R.string.data_size,selectedApp.getDataSize())
        appImage.setImageBitmap(bitmap)
        progressBar.visibility = View.GONE

        internalStoragePath = (this.getExternalFilesDir(null)!!.absolutePath).run {
            substring(0, indexOf("Android")).plus(
                ROOT
            )
        }
        Log.d("internal", internalStoragePath)

        with(FileUtil) {
            createDirectory(internalStoragePath)
            createFile("$internalStoragePath/.nomedia")
        }

        backupButton.setOnClickListener {
            selectedApp.setDate(
                SimpleDateFormat(
                    "dd.MM.yy-HH:mm",
                    Locale.getDefault()
                ).format(Date())
            )
            scope.launch {
                progressBar.visibility = View.VISIBLE
                createLocalBackup(bitmap, selectedApp)
            }
        }

        backupDriveButton.setOnClickListener {
            requestSignIn()
        }

        deleteButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                deleteDialog()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleSignInIntent(data)
                }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun bindViews(binding: ActivityBackupBinding) {
        toolBar = binding.toolbarBackup
        textItem = binding.textItemBackup
        appImage = binding.applicationImageBackup
        apkSize = binding.apkSize
        dataSize = binding.dataSize
        backupButton = binding.backupButton
        backupDriveButton = binding.backupDriveButton
        chipPackage = binding.chipPackageBackup
        chipVersion = binding.chipVersionBackup
        chipDir = binding.chipDirBackup
        deleteButton = binding.floatingDeleteButton
        progressBar = binding.progressBar
    }

    private fun setToolBar() {
        toolBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(toolBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private fun deleteDialog() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        builder.setTitle(getString(R.string.confirm_delete))
        builder.setMessage(getString(R.string.delete_confirmation_message))
        builder.setPositiveButton(getString(R.string.yes)) { dialog, _ ->
            CoroutineScope(Dispatchers.Main).launch {
                dialog.cancel()
                launch {
                    deleteApp(this@BackupActivity, chipPackage.text.toString())
                }.join()
                delay(250)
                onBackPressed()
                Toast.makeText(this@BackupActivity, "Successfully deleted!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        builder.setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.blue))
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.red))
        }
        alert.show()
    }

    private suspend fun createLocalBackup(bitmap: Bitmap, app: Application) {
        println(this.filesDir.absolutePath)
        val bitmapPath = this.filesDir.absolutePath.plus("/${app.getName()}")

        withContext(Dispatchers.IO) {
            val appDir = app.getName().filterNot { it.isWhitespace() }
            val appVersion = app.getVersionName().replace("(", "_").replace(")", "")
            var backupFolder =
                "$internalStoragePath/${appDir}_${appVersion.filterNot { it.isWhitespace() }}"

            with(progressBar) {
                backupFolder = createBackupDir(backupFolder)
                FileUtil.createDirectory(backupFolder.plus("/${app.getPackageName()}"))
                setProgress(25, true)

                SuperUser.sudo("cp -r `ls -d \$PWD${app.getDataDir()}/* | grep -vE \"cache|lib|code_cache\"` $backupFolder/${app.getPackageName()}")
                app.setDataDir(backupFolder)
                app.setDataSize(getLocalDataSize(backupFolder))
                setProgress(45, true)

                copyApk(app.getApkDir(), backupFolder)
                setProgress(75, true)

                copyBitmap(bitmapPath, "${backupFolder}/${app.getName()}.png")
                appToJson(app, backupFolder)
                setProgress(100, true)
            }
            delay(500)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                progressBar.progress = 0
                Toast.makeText(this@BackupActivity, "Done!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createBackupDir(path: String): String {
        val dir = File(path)
        var number = 1
        var newPath = ""
        return if (dir.exists()) {
            while (File(path.plus("_$number")).exists()) {
                number++
            }
            newPath = path.plus("_$number")
            File(newPath).mkdirs()
            newPath
        } else {
            FileUtil.createDirectory(path)
            path
        }
    }

    private fun getLocalDataSize(path: String): Long {
        val dir = File(path)
        return dir.walkTopDown().filter {
            it.isFile
        }.map {
            it.length()
        }.sum()
    }

    private fun appToJson(app: Application, dir: String) {
        val file = File(dir, app.getName().plus(".json"))
        OutputStreamWriter(FileOutputStream(file)).use {
            file.createNewFile()
            it.append(Json.encodeToString(app))
        }
    }

    private fun copyBitmap(source: String, target: String) {
        File(source).copyTo(File(target))
    }

    private fun copyApk(source: String, target: String) {
        val dir = File(source)
        dir.walkTopDown().filter {
            it.absolutePath.contains(".apk")
        }.forEach {
            it.copyTo(File(target.plus(it.absolutePath.removePrefix(source))))
        }
    }

    private suspend fun deleteApp(context: Context, packageName: String) {
        withContext(Dispatchers.Default) {
            val disable = launch {
                val activityManager =
                    applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(packageName)
                //val packageManager = context.packageManager
                //val packageInstaller = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            }
            disable.join()
            delay(300)
        }
    }

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val client = GoogleSignIn.getClient(this, signInOptions)
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun handleSignInIntent(data: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                Log.d(TAG, "Signed in as " + googleAccount.email)
                val credential = GoogleAccountCredential.usingOAuth2(
                    this,
                    Collections.singleton(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("Simple Backup/1.0")
                    .build()
            }
            .addOnFailureListener { exception: java.lang.Exception? ->
                Log.e(TAG, "Unable to sign in.", exception)
            }
    }
}