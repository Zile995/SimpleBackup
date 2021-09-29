package com.stefan.simplebackup.backup

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.Application
import java.io.*

class BackupActivity : AppCompatActivity() {

    private lateinit var textItem: MaterialTextView
    private lateinit var appImage: ImageView
    private lateinit var backupButton: MaterialButton
    private lateinit var backupDriveButton: MaterialButton
    private lateinit var chipVersion: Chip
    private lateinit var chipPackage: Chip
    private lateinit var chipDir: Chip
    private lateinit var deleteButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        textItem = findViewById(R.id.text_item_backup)
        appImage = findViewById(R.id.application_image_backup)
        backupButton = findViewById(R.id.backup_button)
        backupDriveButton = findViewById(R.id.backup_drive_button)
        chipPackage = findViewById(R.id.chip_package_restore)
        chipVersion = findViewById(R.id.chip_version_restore)
        chipDir = findViewById(R.id.chip_dir_restore)
        deleteButton = findViewById(R.id.floating_delete_button)


        val selectedApp: Application? = intent?.extras?.getParcelable("application")
        Log.d("selected", selectedApp.toString())

        textItem.text = selectedApp?.getName()
        chipPackage.text = (selectedApp?.getPackageName() as CharSequence).toString()
        chipVersion.text = (selectedApp.getVersionName() as CharSequence).toString()
        chipDir.text = (selectedApp.getDataDir() as CharSequence).toString()

        val bitmap = BitmapFactory.decodeStream(this.openFileInput(selectedApp.getName()))
        appImage.setImageBitmap(bitmap)

        Log.d("bundle:", getApkBundlePath(chipPackage.text.toString()))

        backupButton.setOnClickListener {
            createBackup(bitmap)
        }

        deleteButton.setOnClickListener {
            deleteDialog()
        }
    }

    private fun deleteDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.confirm_delete))
        builder.setMessage(getString(R.string.delete_confirmation_message))
        builder.setPositiveButton(getString(R.string.yes)) { dialog, _ ->
            deleteApp(chipPackage.text.toString())
            dialog.cancel()
            onBackPressed()
        }
        builder.setNegativeButton(getString(R.string.no)) { dialog, _ -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    private fun createBackup(bitmap: Bitmap) {
        val backupFolder = "/storage/emulated/0/SimpleBackup/${textItem.text}_${chipVersion.text}"
        sudo("mkdir -p $backupFolder")
        sudo("cp -r /data/data/${chipPackage.text} $backupFolder/")
        sudo("cp -r ${getApkBundlePath(chipPackage.text.toString())}*.apk $backupFolder/")
        saveBitmap(bitmap, "${backupFolder}/${textItem.text}")
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

    private fun saveBitmap(bitmap: Bitmap?, path: String) {
        if (bitmap != null) {
            try {
                var outputStream: FileOutputStream? = null
                try {
                    outputStream = FileOutputStream(path)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        outputStream?.close()
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
        return try {
            val process = Runtime.getRuntime().exec("su -c pm path $packageName | cut -d':' -f2")
            val apkPath = BufferedReader(InputStreamReader(process.inputStream)).readLine()
            val string = apkPath.removeSuffix("base.apk")
            string
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        } catch (e: InterruptedException) {
            e.printStackTrace()
            ""
        }
    }

    private fun deleteApp(packageName: String) {
        try {
            sudo("pm uninstall $packageName")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}