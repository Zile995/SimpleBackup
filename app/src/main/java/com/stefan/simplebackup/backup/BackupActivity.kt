package com.stefan.simplebackup.backup

import android.app.ActivityManager
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

        private lateinit var dataPath: String
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

    private lateinit var bitmap: Bitmap

    private var selectedApp: Application? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        val binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindViews(binding)
        setCardViewSize()

        scope.launch {
            selectedApp = intent?.extras?.getParcelable("application")

            if (selectedApp != null) {
                with(selectedApp!!) {
                    val dataSize = calculateDataSize(getDataDir())
                    setDataSize(dataSize)
                    bitmap = this@BackupActivity.getAppBitmap(this)
                    // Set UI data
                    setUI(this)
                }
            }
            launch {
                internalStoragePath =
                    (this@BackupActivity.getExternalFilesDir(null)!!.absolutePath).run {
                        substring(0, indexOf("Android")).plus(
                            ROOT
                        )
                    }
                Log.d("internal", internalStoragePath)
            }
            launch {
                withContext(Dispatchers.Default) {
                    with(FileUtil) {
                        createDirectory(internalStoragePath)
                        createFile("$internalStoragePath/.nomedia")
                    }
                }
            }
        }
    }

    private fun setUI(app: Application) {
        with(app) {
            textItem.text = getName()
            chipPackage.text = (getPackageName() as CharSequence).toString()
            chipVersion.text = (getVersionName() as CharSequence).toString()
            chipDir.text = (getDataDir() as CharSequence).toString()
            chipApkSize.text = FileUtil.transformBytes(getApkSize())
            chipTargetSdk.text = getTargetSdk().toString()
            chipMinSdk.text = getMinSdk().toString()
            dataPath = getDataDir()
            chipDataSize.text = selectedApp?.getDataSize()
            appImage.setImageBitmap(bitmap)
        }
    }

    private suspend fun getAppBitmap(app: Application): Bitmap {
        var bitmap = app.getBitmapFromArray()
        withContext(Dispatchers.IO) {
            runCatching {
                if (bitmap == null) {
                    bitmap =
                        BitmapFactory.decodeStream(this@BackupActivity.openFileInput(app.getName()))
                    this@BackupActivity.deleteFile(app.getName())
                    app.setBitmap(FileUtil.bitmapToByteArray(bitmap!!))
                }
            }
        }
        return bitmap!!
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
        chipPackage = binding.chipPackageBackup
        chipVersion = binding.chipVersionBackup
        chipDir = binding.chipDirBackup
        progressBar = binding.progressBar
        cardView = binding.cardView
        cardViewPackage = binding.cardViewPackage
        cardViewButtons = binding.cardViewButtons
        createBackupButton(binding)
        createBackupDriveButton(binding)
        createDeleteButton(binding)
        setToolBar()
    }

    private fun createBackupButton(binding: ActivityBackupBinding) {
        backupButton = binding.backupButton

        backupButton.setOnClickListener {
            selectedApp!!.setDate(
                SimpleDateFormat(
                    "dd.MM.yy-HH:mm",
                    Locale.getDefault()
                ).format(Date())
            )
            scope.launch {
                progressBar.visibility = View.VISIBLE
                createLocalBackup(selectedApp!!)
            }
        }
    }

    private fun createBackupDriveButton(binding: ActivityBackupBinding) {
        backupDriveButton = binding.backupDriveButton

        backupDriveButton.setOnClickListener {
            requestSignIn()
        }

    }

    private fun createDeleteButton(binding: ActivityBackupBinding) {
        deleteButton = binding.floatingDeleteButton

        deleteButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                onBackPressed()
                delay(150)
                launch {
                    deleteApp(selectedApp!!.getPackageName())
                }.join()
            }
        }
    }

    private fun setCardViewSize() {
        CoroutineScope(Dispatchers.Default).launch {
            val height =
                constraintLayout.height - toolBar.height - cardView.height - cardViewPackage.height
            cardViewButtons.layoutParams.height = height
        }
    }

    private fun setToolBar() {
        toolBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(toolBar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
    }

    private suspend fun calculateDataSize(path: String): String {
        return withContext(Dispatchers.IO) {
            if (Shell.rootAccess()) {
                val resultList = arrayListOf<String>()
                var result = ""
                Shell.su("du -sch $path/").to(resultList).exec()
                resultList.forEach {
                    if (it.contains("total")) {
                        result = it.removeSuffix("\ttotal")
                    }
                }
                if (result.equals("16K"))
                    result = "0K"

                result = StringBuilder(result)
                    .insert(result.length - 1, " ")
                    .append("B")
                    .toString()

                result
            } else
                "Can't read"
        }
    }

    private suspend fun createLocalBackup(app: Application) {
        withContext(Dispatchers.IO) {
            val appName = app.getName()
            val appVersion = app.getVersionName()
            var backupFolder =
                "$internalStoragePath/${appName}_${appVersion}"

            with(progressBar) {
                backupFolder = createBackupDir(backupFolder)
                app.setDataDir(backupFolder)
                setProgress(5, true)

                launch {
                    val apkBackupTar = backupFolder.plus("/$appName.tar.gz")
                    val packageBackupTar = backupFolder.plus("/${app.getPackageName()}.tar.gz")
                    if (Shell.rootAccess()) {
                        Shell.su("x=$(echo -e \"$apkBackupTar\") && tar -zcf \"\$x\" --exclude=lib --exclude=oat -C ${app.getApkDir()} . ")
                            .exec()
                        setProgress(25, true)
                        Shell.su("x=\$(echo -e \"$packageBackupTar\") && tar -zcf \"\$x\" --exclude={\"cache\",\"lib\",\"code_cache\"} -C $dataPath . ")
                            .exec()
                        setProgress(50, true)
                    } else {
                        copyApk(app.getApkDir(), backupFolder)
                    }
                    app.setDataSize(FileUtil.transformBytes(getLocalDataSize(packageBackupTar)))
                    appToJson(app, backupFolder)
                    setProgress(75, true)
                }.join()
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

//    private fun deleteApk(apkPath: String) {
//        val dir = File(apkPath)
//        dir.walkTopDown().filter {
//            it.absolutePath.contains(".apk")
//        }.forEach {
//            it.delete()
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

    private fun getLocalDataSize(path: String): Float {
        val result = arrayListOf<String>()
        Shell.su("x=$(echo -e \"$path\") && gzip -d \"\$x\" -c|wc -c").to(result).exec()
        return if (result.isNotEmpty()) {
            result.first().toFloat()
        } else
            0f
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

    private fun deleteApp(packageName: String) {
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