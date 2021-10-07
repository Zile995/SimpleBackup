package com.stefan.simplebackup.backup

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.util.*


class BackupActivity : AppCompatActivity() {

    companion object {
        private const val TAG: String = "BackupActivity"
        private const val REQUEST_CODE_SIGN_IN: Int = 400
    }

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
        chipPackage = findViewById(R.id.chip_package_restore)
        chipVersion = findViewById(R.id.chip_version_restore)
        chipDir = findViewById(R.id.chip_dir_restore)
        deleteButton = findViewById(R.id.floating_delete_button)
        progressBar = findViewById(R.id.progress_bar)

        topBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(topBar)

        val selectedApp: Application? = intent?.extras?.getParcelable("application")
        val bitmap = BitmapFactory.decodeStream(this.openFileInput(selectedApp?.getName()))

        textItem.text = selectedApp?.getName()
        chipPackage.text = (selectedApp?.getPackageName() as CharSequence).toString()
        chipVersion.text = (selectedApp.getVersionName() as CharSequence).toString()
        chipDir.text = (selectedApp.getDataDir() as CharSequence).toString()
        appImage.setImageBitmap(bitmap)
        progressBar.visibility = View.GONE

        backupButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            createLocalBackup(bitmap, selectedApp)
        }

        backupDriveButton.setOnClickListener {
            requestSignIn()
        }

        deleteButton.setOnClickListener {
            deleteDialog()
        }
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
            deleteApp(chipPackage.text.toString())
            dialog.cancel()
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, "Successfully deleted!", Toast.LENGTH_SHORT).show()
            }, 1500)
            onBackPressed()
        }
        builder.setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
        val alert = builder.create()
        alert.setOnShowListener(object : DialogInterface.OnShowListener {
            override fun onShow(dialog: DialogInterface?) {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(resources.getColor(R.color.white))
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(resources.getColor(R.color.white))
            }
        })
        alert.show()
    }

    private fun createLocalBackup(bitmap: Bitmap, app: Application) {
        val root = "/storage/emulated/0/SimpleBackup"
        val backupFolder = "$root/${app.getName().filterNot { it.isWhitespace() }}_${
            app.getVersionName().filterNot { it.isWhitespace() }
        }"
        sudo("mkdir -p $backupFolder")
        progressBar.setProgress(25, true)
        sudo("cp -r /data/data/${app.getPackageName()} $backupFolder/")
        progressBar.setProgress(45, true)
        sudo("cp -r ${getApkBundlePath(app.getPackageName())}*.apk $backupFolder/")
        progressBar.setProgress(75, true)
        saveBitmap(bitmap, "${backupFolder}/${app.getName()}.png")
        progressBar.setProgress(100, true)
        Handler(Looper.getMainLooper()).postDelayed({
            progressBar.visibility = View.GONE
            progressBar.progress = 0
            Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show()
        }, 1000)
        saveJsonString(appToJsonString(app),backupFolder,app)
    }

    private fun appToJsonString(app: Application): String {
        return Json.encodeToString(app)
    }

    private fun saveJsonString(json: String, dir: String, app: Application) {
        val file = File(dir, app.getName().plus(".txt"))
        file.createNewFile()
        OutputStreamWriter(FileOutputStream(file)).use {
            it.append(json)
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

    private fun getApkBundlePath(packageName: String): String {
            val process =
                Runtime.getRuntime().exec("su -c pm path $packageName | cut -d':' -f2")
            BufferedReader(InputStreamReader(process.inputStream)).use {
                return it.readLine().removeSuffix("base.apk")
            }
        }

    private fun deleteApp(packageName: String) {
        try {
            sudo("pm uninstall $packageName")
        } catch (e: Exception) {
            e.printStackTrace()
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

//    private fun getPermissions(packageName: String): String {
//        return try {
//            var string = ""
//            val process = Runtime.getRuntime().exec(
//                "su -c " +
//                        "cat /data/system/packages.list | awk '{print \"u0_a\" \$2-10000 \":u0_a\" \$2-10000 \" /data/data/\"\$1\"\"}'"
//            )
//            val buffer = BufferedReader(InputStreamReader(process.inputStream))
//            buffer.forEachLine {
//                if (it.contains(chipPackage.text)) {
//                    string = it
//                }
//            }
//            string
//        } catch (e: IOException) {
//            e.printStackTrace()
//            return ""
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//            return ""
//        }
//    }

}