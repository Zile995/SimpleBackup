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
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.math.pow

class RestoreAdapter(rContext: Context) : RecyclerView.Adapter<RestoreAdapter.RestoreViewHolder>() {

    companion object {
        private const val ROOT: String = "/storage/emulated/0/SimpleBackup"
        private const val LOCAL: String = "/data/local/tmp"
        private const val DATA: String = "/data/data"
    }

    private var appList = mutableListOf<Application>()
    private var bitmapList = mutableListOf<ApplicationBitmap>()
    private var context = rContext

    class RestoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var cardView: MaterialCardView
        var textItem: MaterialTextView
        var appSize: MaterialTextView
        var dateText: MaterialTextView
        var appImage: ImageView
        var chipVersion: Chip

        init {
            cardView = view.findViewById(R.id.card_item)
            textItem = view.findViewById(R.id.text_item)
            appSize = view.findViewById(R.id.app_size_text)
            appImage = view.findViewById(R.id.application_image)
            chipVersion = view.findViewById(R.id.chip_version)
            dateText = view.findViewById(R.id.date_text)
        }
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
        holder.appSize.text = transformBytes(item.getSize())
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
                    .setTextColor(context.resources.getColor(R.color.white))
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(context.getColor(R.color.white))
            }
            alert.show()
        }
    }

    override fun getItemCount() = this.appList.size

    private suspend fun installApp(context: Context, application: Application) {
        withContext(Dispatchers.IO) {
            val backupDir = application.getDataDir()
            val restoreDir = LOCAL.plus(backupDir.removePrefix(ROOT))
            val packageName = application.getPackageName()
            val dataDir = "$DATA/$packageName"
            try {
                with(Installer) {
                    sudo("mkdir -p $restoreDir")
                    sudo("cp -r $backupDir/*.apk $restoreDir/")
                    sudo("rm -rf $dataDir")
                    installApk(context, restoreDir)
                    sudo("cp -r $backupDir/$packageName $DATA/")
                    sudo(getPermissions(packageName))
                    sudo("restorecon -R $dataDir")
                    sudo("rm -rf $restoreDir")
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
            sudo("pm install-create -S $totalSize")
            val sessions = packageInstaller.allSessions
            val sessionId = sessions[0].sessionId
            for ((apk, size) in apkSizeMap) {
                sudo(
                    "pm install-write -S $size $sessionId ${apk.name} ${apk.absolutePath}"
                )
            }
            sudo("pm install-commit $sessionId")
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

        fun sudo(vararg strings: String) {
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
    }

    private fun transformBytes(bytes: Long): String {
        return String.format("%3.2f %s", bytes / 1000.0.pow(2), "MB")
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