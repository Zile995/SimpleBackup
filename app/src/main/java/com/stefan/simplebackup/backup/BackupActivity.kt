package com.stefan.simplebackup.backup

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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

class BackupActivity : AppCompatActivity() {

    companion object {
        private const val ROOT: String = "SimpleBackup/local"
        private const val TAG: String = "BackupActivity"
        private const val REQUEST_CODE_SIGN_IN: Int = 400
    }

    private var internalStoragePath: String = ""
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var toolBar: Toolbar
    private lateinit var textItem: MaterialTextView
    private lateinit var appImage: ImageView
    private lateinit var chipApkSize: Chip
    private lateinit var chipDataSize: Chip
    private lateinit var chipTargetSdk: Chip
    private lateinit var chipMinSdk: Chip
    private lateinit var backupButton: MaterialButton
    private lateinit var backupDriveButton: MaterialButton
    private lateinit var chipVersion: Chip
    private lateinit var chipPackage: Chip
    private lateinit var chipDir: Chip
    private lateinit var deleteButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var cardView: MaterialCardView
    private lateinit var cardViewPackage: MaterialCardView
    private lateinit var cardViewButtons: MaterialCardView

    private var selectedApp: Application? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        val binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind Views
        bindViews(binding)
        setToolBar()

        CoroutineScope(Dispatchers.Default).launch {
            val height =
                constraintLayout.height - toolBar.height - cardView.height - cardViewPackage.height
            cardViewButtons.layoutParams.height = height
        }

        selectedApp = intent?.extras?.getParcelable("application")

        if (selectedApp != null) {
            with(selectedApp!!) {
                textItem.text = getName()
                chipPackage.text = (getPackageName() as CharSequence).toString()
                chipVersion.text = (getVersionName() as CharSequence).toString()
                chipDir.text = (getDataDir() as CharSequence).toString()
                chipApkSize.text = FileUtil.transformBytes(getApkSize())
                chipTargetSdk.text = getTargetSdk().toString()
                chipMinSdk.text = getMinSdk().toString()
                chipDataSize.text = getDataSize()
                var bitmap = getBitmap()
                if (bitmap == null) {
                    bitmap =
                        BitmapFactory.decodeStream(this@BackupActivity.openFileInput(getName()))
                    this@BackupActivity.deleteFile(getName())
                }
                setBitmap(FileUtil.bitmapToByteArray(bitmap!!))
                appImage.setImageBitmap(bitmap)
            }
        }
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
            selectedApp!!.setDate(
                SimpleDateFormat(
                    "dd.MM.yy-HH:mm",
                    Locale.getDefault()
                ).format(Date())
            )
            scope.launch {
                progressBar.visibility = View.VISIBLE
                createLocalBackup(selectedApp!!, selectedApp!!.getBitmap())
            }
        }

        backupDriveButton.setOnClickListener {
            requestSignIn()
        }

        deleteButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                onBackPressed()
                delay(150)
                launch {
                    deleteApp(this@BackupActivity, selectedApp!!.getPackageName())
                }.join()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_backup_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.force_stop -> {
                forceStop(selectedApp!!.getPackageName())
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
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
                if (resultCode == RESULT_OK && data != null) {
                    handleSignInIntent(data)
                }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun bindViews(binding: ActivityBackupBinding) {
        constraintLayout = binding.parentView
        toolBar = binding.toolbarBackup
        textItem = binding.textItemBackup
        appImage = binding.applicationImageBackup
        chipApkSize = binding.apkSize
        chipDataSize = binding.dataSize
        chipTargetSdk = binding.targetSdk
        chipMinSdk = binding.minSdk
        backupButton = binding.backupButton
        backupDriveButton = binding.backupDriveButton
        chipPackage = binding.chipPackageBackup
        chipVersion = binding.chipVersionBackup
        chipDir = binding.chipDirBackup
        deleteButton = binding.floatingDeleteButton
        progressBar = binding.progressBar
        cardView = binding.cardView
        cardViewPackage = binding.cardViewPackage
        cardViewButtons = binding.cardViewButtons
    }

    private fun setToolBar() {
        toolBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(toolBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private suspend fun createLocalBackup(app: Application, bitmap: Bitmap?) {
        withContext(Dispatchers.IO) {
            val appDir = app.getName().filterNot { it.isWhitespace() }
            val appVersion = app.getVersionName().replace("(", "_").replace(")", "")
            var backupFolder =
                "$internalStoragePath/${appDir}_${appVersion.filterNot { it.isWhitespace() }}"

            with(progressBar) {
                backupFolder = createBackupDir(backupFolder)
                setProgress(25, true)

                if (Shell.rootAccess()) {
                    val packageBackupFolder = backupFolder.plus("/${app.getPackageName()}")
                    FileUtil.createDirectory(packageBackupFolder)
                    SuperUser.sudo("cp -dR `ls -d \$PWD${app.getDataDir()}/* | grep -vE \"Android|cache|lib|code_cache\"` $packageBackupFolder/")
                }

                app.setDataDir(backupFolder)
                app.setDataSize(getLocalDataSize(backupFolder))
                setProgress(45, true)

                copyApk(app.getApkDir(), backupFolder)
                setProgress(75, true)

                appToJson(app, backupFolder)
                setProgress(100, true)
            }
            delay(500)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.INVISIBLE
                progressBar.progress = 0
                Toast.makeText(this@BackupActivity, "Done!", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    private fun checkBackup(path: String, app: Application) {
//        val dir = File(path)
//        var count = 0
//        dir.listFiles()?.forEach {
//            it.isDirectory
//        }. {
//            this.forEach {
//                it.name.contains(app.getName())
//            }.apply { count++ }
//        }
//    }

    private fun createBackupDir(path: String): String {
        val dir = File(path)
        var number = 1
        val newPath: String
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

    private fun getLocalDataSize(path: String): String {
        val dir = File(path)
        val size = dir.walkTopDown().filter {
            it.isFile
        }.map {
            it.length()
        }.sum()
        return FileUtil.transformBytes(size.toFloat())
    }

    private fun appToJson(app: Application, dir: String) {
        val file = File(dir, app.getName().plus(".json"))
        OutputStreamWriter(FileOutputStream(file)).use {
            file.createNewFile()
            it.append(Json.encodeToString(app))
        }
    }

    private fun copyApk(source: String, target: String) {
        val dir = File(source)
        dir.walkTopDown().filter {
            it.absolutePath.contains(".apk")
        }.forEach {
            it.copyTo(File(target.plus(it.absolutePath.removePrefix(source))))
        }
    }

    private fun deleteApp(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:${packageName}")
        startActivity(intent)
    }

    private fun forceStop(packageName: String) {
        val activityManager =
            applicationContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses(packageName)
        Toast.makeText(this@BackupActivity, "Application stopped!", Toast.LENGTH_SHORT).show()
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
            .addOnFailureListener { exception: Exception? ->
                Log.e(TAG, "Unable to sign in.", exception)
            }
    }
}