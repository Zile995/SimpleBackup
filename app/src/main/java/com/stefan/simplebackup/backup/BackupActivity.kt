package com.stefan.simplebackup.backup

import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.os.Build
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
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.*


class BackupActivity : AppCompatActivity() {

    companion object {
        private const val TAG: String = "BackupActivity"
        private const val REQUEST_CODE_SIGN_IN: Int = 400
    }

    var internalStoragePath: String = ""
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var topBar: Toolbar
    private lateinit var textItem: MaterialTextView
    private lateinit var appImage: ImageView
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

        topBar = findViewById(R.id.top_backup_bar)
        textItem = findViewById(R.id.text_item_backup)
        appImage = findViewById(R.id.application_image_backup)
        backupButton = findViewById(R.id.backup_button)
        backupDriveButton = findViewById(R.id.backup_drive_button)
        chipPackage = findViewById(R.id.chip_package_backup)
        chipVersion = findViewById(R.id.chip_version_backup)
        chipDir = findViewById(R.id.chip_dir_backup)
        deleteButton = findViewById(R.id.floating_delete_button)
        progressBar = findViewById(R.id.progress_bar)

        topBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(topBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        val selectedApp: Application? = intent?.extras?.getParcelable("application")
        val bitmap = BitmapFactory.decodeStream(this.openFileInput(selectedApp?.getName()))

        textItem.text = selectedApp?.getName()
        chipPackage.text = (selectedApp?.getPackageName() as CharSequence).toString()
        chipVersion.text = (selectedApp.getVersionName() as CharSequence).toString()
        chipDir.text = (selectedApp.getDataDir() as CharSequence).toString()
        appImage.setImageBitmap(bitmap)
        progressBar.visibility = View.GONE

        createDirectory(internalStoragePath)
        createFile("$internalStoragePath/.nomedia")

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
                .setTextColor(resources.getColor(R.color.white))
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.white))
        }
        alert.show()
    }

    private suspend fun createLocalBackup(bitmap: Bitmap, app: Application) {
        withContext(Dispatchers.IO) {
            val appDir = app.getName().filterNot { it.isWhitespace() }
            val appVersion = app.getVersionName().replace("(", "_").replace(")", "")
            var backupFolder =
                "$internalStoragePath/${appDir}_${appVersion.filterNot { it.isWhitespace() }}"

            with(progressBar) {
                backupFolder = createBackupDir(backupFolder)
                setProgress(25, true)

                // sudo("cp -r `ls -d \$PWD${app.getDataDir()}/* | grep -vE \"cache|code_cache\"` $backupFolder/")
                sudo("cp -r ${app.getDataDir()} $backupFolder/")
                app.setDataDir(backupFolder)
                app.setSize(getDataSize(backupFolder))
                setProgress(45, true)

                sudo("cp ${app.getApkDir()}/*.apk $backupFolder/")
                setProgress(75, true)

                saveBitmap(bitmap, "${backupFolder}/${app.getName()}.png")
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
            createDirectory(path)
            path
        }
    }

    private fun createDirectory(path: String) {
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun createFile(path: String) {
        val file = File(path)
        file.createNewFile()
    }

    private fun getDataSize(path: String): Long {
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

    private fun saveBitmap(bitmap: Bitmap?, path: String) {
        if (bitmap != null) {
            try {
                val outputStream = FileOutputStream(path)
                try {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        outputStream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sudo(vararg strings: String) {
        try {
            val su = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(su.outputStream)
            for (s in strings) {
                outputStream.writeBytes(s + "\n")
                Log.d("command", "$s\n")
                outputStream.flush()
            }

            outputStream.writeBytes("exit\n")
            outputStream.flush()
            try {
                su.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//    private fun getApkBundlePath(packageName: String): String {
//        val process =
//            Runtime.getRuntime().exec("su -c pm path $packageName | cut -d':' -f2")
//        BufferedReader(InputStreamReader(process.inputStream)).use {
//            return it.readLine().removeSuffix("base.apk")
//        }
//    }

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
            sudo("pm uninstall $packageName")
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