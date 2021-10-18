package com.stefan.simplebackup.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.backup.BackupActivity
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.pow

class AppAdapter() : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    private var appList = mutableListOf<Application>()
    private var bitmapList = mutableListOf<ApplicationBitmap>()

    class AppViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var cardView: MaterialCardView
        var textItem: MaterialTextView
        var appSize: MaterialTextView
        var appImage: ImageView
        var chipVersion: Chip
        var chipPackage: Chip

        init {
            cardView = view.findViewById(R.id.card_item)
            textItem = view.findViewById(R.id.text_item)
            appSize = view.findViewById(R.id.app_size_text)
            appImage = view.findViewById(R.id.application_image)
            chipVersion = view.findViewById(R.id.chip_version)
            chipPackage = view.findViewById(R.id.chip_package)
        }

    }

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz liste
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        // Parent parametar, je view grupa za koju će da se zakači list item view kao child view. Parent je RecyclerView.
        // layout sadži referencu na child view (list_item) koji je zakačen na parent view (RecyclerView)
        val layout = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        // Vrati ViewHolder
        return AppViewHolder(layout)
    }

    /**
     * - Služi da postavimo parametre
     */
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val context = holder.view.context
        val item = appList[position]
        val bitmap = bitmapList[position]
        val charSequencePackage: CharSequence = item.getPackageName()
        val charSequenceVersion: CharSequence = "v" + item.getVersionName()

        holder.textItem.text = item.getName()
        holder.appImage.setImageBitmap(bitmap.getIcon())
        holder.chipVersion.text = charSequenceVersion.toString()
        holder.appSize.text = transformBytes(item.getSize())

        saveBitmap(bitmap.getIcon(), item.getName(), context)
        holder.chipPackage.text = charSequencePackage.toString()
        holder.cardView.setOnClickListener {
            val intent = Intent(context, BackupActivity::class.java)
            intent.putExtra("application", item)
            context.startActivity(intent)
        }
    }


    override fun getItemCount() = this.appList.size

    private fun saveBitmap(bitmap: Bitmap?, fileName: String, context: Context) {
        try {
            val bytes = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, bytes)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
                output.write(bytes.toByteArray())
                output.close()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun transformBytes(bytes: Long): String {
        return String.format("%3.2f %s", bytes / 1000.0.pow(2), "MB")
    }

    fun updateList(
        newList: MutableList<Application>,
        newBitmapList: MutableList<ApplicationBitmap>
    ) {
        appList = newList
        bitmapList = newBitmapList
        notifyDataSetChanged()
    }
}