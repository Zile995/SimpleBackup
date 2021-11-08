package com.stefan.simplebackup.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.backup.BackupActivity
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import com.stefan.simplebackup.utils.SuperUser
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.math.pow

class RestoreAdapter(rContext: Context) : RecyclerView.Adapter<RestoreAdapter.RestoreViewHolder>() {

    companion object {
        private const val ROOT: String = "SimpleBackup/local"
        private const val LOCAL: String = "/data/local/tmp"
        private const val DATA: String = "/data/data"
    }

    private var appList = mutableListOf<Application>()
    private var bitmapList = mutableListOf<ApplicationBitmap>()
    private var context = rContext

    class RestoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var cardView: MaterialCardView = view.findViewById(R.id.card_item)
        var textItem: MaterialTextView = view.findViewById(R.id.text_item)
        var appSize: MaterialTextView = view.findViewById(R.id.app_size_text)
        var dateText: MaterialTextView = view.findViewById(R.id.date_text)
        var appImage: ImageView = view.findViewById(R.id.application_image)
        var chipVersion: Chip = view.findViewById(R.id.chip_version)
    }

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz liste
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        // Parent parametar, je view grupa za koju će da se zakači list item view kao child view. Parent je RecyclerView.
        // layout sadži referencu na child view (list_item) koji je zakačen na parent view (RecyclerView)
        val layout = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.restore_item, parent, false)

        // Vrati ViewHolder
        return RestoreViewHolder(layout)
    }

    /**
     * - Služi da postavimo parametre
     */
    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {

        val item = appList[position]
        val bitmap = bitmapList[position]
        val charSequenceVersion: CharSequence = "v" + item.getVersionName()

        holder.textItem.text = item.getName()
        holder.appImage.setImageBitmap(bitmap.getIcon())
        holder.chipVersion.text = charSequenceVersion.toString()
        holder.appSize.text = item.getDataSize().plus("MB")
        holder.dateText.text = item.getDate()

        holder.cardView.setOnClickListener {
            val builder = AlertDialog.Builder(context, R.style.DialogTheme)
            builder.setTitle(context.getString(R.string.confirm_restore))
            builder.setMessage(context.getString(R.string.restore_confirmation_message))
            builder.setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
                dialog.cancel()
                CoroutineScope(Dispatchers.Main).launch {
                    launch { installApp(context, item) }.join()
                    launch {
                        Toast.makeText(context, "Successfully restored!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            builder.setNegativeButton(context.getString(R.string.no)) { dialog, _ -> dialog.cancel() }
            val alert = builder.create()
            alert.setOnShowListener {
                alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(context.resources.getColor(R.color.red))
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(context.getColor(R.color.blue))
            }
            alert.show()
        }
    }

    override fun getItemCount() = this.appList.size

    private suspend fun installApp(context: Context, application: Application) {
        withContext(Dispatchers.IO) {
            val internalStoragePath = (context.getExternalFilesDir(null)!!.absolutePath).run {
                substring(0, indexOf("Android")).plus(
                    ROOT
                )
            }
            println(internalStoragePath)
            val backupDir = application.getDataDir()
            val tempDir = LOCAL.plus(backupDir.removePrefix(internalStoragePath))
            println(tempDir)
            val packageName = application.getPackageName()
            val dataDir = "$DATA/$packageName"
            try {
                with(Installer) {
                    Shell.su("mkdir -p $tempDir").exec()
                    Shell.su("cp -r $backupDir/*.apk $tempDir/").exec()
                    Shell.su("rm -rf $dataDir").exec()
                    installApk(context, tempDir)
                    Shell.su("cp -r $backupDir/$packageName $DATA/").exec()
                    Shell.su(getPermissions(packageName)).exec()
                    Shell.su("restorecon -R $dataDir").exec()
                    Shell.su("rm -rf $tempDir").exec()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    object Installer {
        @JvmStatic
        fun installApk(context: Context, apkFolderPath: String) {
            val packageInstaller = context.packageManager.packageInstaller
            val apkSizeMap = HashMap<File, Long>()
            var totalSize: Long = 0
            val folder = File(apkFolderPath)
            val listOfFiles =
                folder.listFiles()?.filter { it.isFile && it.name.endsWith(".apk") }
            listOfFiles?.forEach {
                apkSizeMap[it] = it.length()
                totalSize += it.length()
            }
            Shell.su("pm install-create -S $totalSize").exec()
            val sessions = packageInstaller.allSessions
            val sessionId = sessions[0].sessionId
            for ((apk, size) in apkSizeMap) {
                Shell.su(
                    "pm install-write -S $size $sessionId ${apk.name} ${apk.absolutePath}"
                ).exec()
            }
            Shell.su("pm install-commit $sessionId").exec()
        }

        fun getPermissions(packageName: String): String {
            var line = ""
            val process = Runtime.getRuntime().exec(
                "su -c " +
                        "cat /data/system/packages.list | awk '{print \"chown u0_a\" \$2-10000 \":u0_a\" \$2-10000 \" /data/data/\"\$1\" -R\"}'"
            )
            BufferedReader(InputStreamReader(process.inputStream)).use { buffer ->
                buffer.forEachLine {
                    if (it.contains(packageName)) {
                        line = it
                    }
                }
            }
            println(line)
            return line
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(
        newList: MutableList<Application>,
        newBitmapList: MutableList<ApplicationBitmap>,
        rContext: Context
    ) {
        appList = newList
        bitmapList = newBitmapList
        context = rContext
        notifyDataSetChanged()
    }

}