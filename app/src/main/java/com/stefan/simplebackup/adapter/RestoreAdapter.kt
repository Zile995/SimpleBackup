package com.stefan.simplebackup.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.AppData
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class RestoreAdapter(rContext: Context) :
    ListAdapter<AppData, RestoreAdapter.RestoreViewHolder>(AppAdapter.AppDiffCallBack), Filterable {

    companion object {
        private const val ROOT: String = "SimpleBackup/local"
        private const val LOCAL: String = "/data/local/tmp"
        private const val DATA: String = "/data/data"
    }

    private var appList = currentList
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
        val item = getItem(position)
        val bitmap = item.getBitmapFromArray()
        val charSequenceVersion: CharSequence = "v" + item.getVersionName()

        holder.textItem.text = item.getName()
        holder.appImage.setImageBitmap(bitmap)
        holder.chipVersion.text = charSequenceVersion.toString()
        holder.appSize.text = item.getDataSize()
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
                    .setTextColor(context.getColor(R.color.negativeDialog))
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(context.getColor(R.color.positiveDialog))
            }
            alert.show()
        }
    }

    fun setData(list: MutableList<AppData>) {
        this.appList = list
        submitList(appList)
    }

    override fun getFilter(): Filter {
        return appFilter
    }

    private val appFilter = object : Filter() {
        override fun performFiltering(sequence: CharSequence?): FilterResults {
            val filteredList = mutableListOf<AppData>()
            if (sequence.isNullOrBlank()) {
                filteredList.addAll(appList)
            } else {
                appList.forEach() {
                    if (it.getName().lowercase().contains(sequence.toString().lowercase().trim())) {
                        filteredList.add(it)
                    }
                }
            }
            val results = FilterResults()
            results.values = filteredList
            return results
        }

        override fun publishResults(sequence: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            submitList(results?.values as MutableList<AppData>)
        }

    }

    private suspend fun installApp(context: Context, app: AppData) {
        val libDir = context.applicationInfo.nativeLibraryDir

        withContext(Dispatchers.IO) {
            val internalStoragePath = (context.getExternalFilesDir(null)!!.absolutePath).run {
                substring(0, indexOf("Android")).plus(
                    ROOT
                )
            }
            println(internalStoragePath)
            val backupDir = app.getDataDir()
            val tempDir = LOCAL.plus(backupDir.removePrefix(internalStoragePath))
            println(tempDir)
            val packageName = app.getPackageName()
            val packageDataDir = "$DATA/$packageName"
            try {
                with(Installer) {
//                    Shell.su("x=$(echo -e \"$tempDir\") && mkdir -p \"\$x\"").exec()
//                    Shell.su("x=$(echo -e \"$backupDir/${app.getName()}.tar.gz\")" +
//                            " && y=$(echo -e \"$tempDir/\")" +
//                            " && $libDir/libtar.so -zxf \"\$x\" -C \"\$y\"").exec()
                    Shell.su("rm -rf $packageDataDir/*").exec()
                    //installApk(context, tempDir)
                    Shell.su("x=$(echo -e \"$backupDir/${app.getPackageName()}.tar.gz\") && $libDir/libtar.so -zxf \"\$x\" -C $packageDataDir/ --recursive-unlink --preserve-permissions\n --preserve-permissions")
                        .exec()
                    //Shell.su(getPermissions(packageName)).exec()
                    //Shell.su("restorecon -R $packageDataDir").exec()
                    //Shell.su("x=$(echo -e \"$tempDir\") && rm -rf \"\$x\"").exec()
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
                val apkFilePath = "x=$(echo -e \"${apk.absolutePath}\")"
                Shell.su(
                    "$apkFilePath && pm install-write -S $size $sessionId ${apk.name} \"\$x\""
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

}