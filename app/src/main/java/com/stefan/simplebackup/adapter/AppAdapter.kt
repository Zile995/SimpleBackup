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
    private var isMainActivity = true

    class AppViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        lateinit var cardView: MaterialCardView
        lateinit var textItem: MaterialTextView
        lateinit var appSize: MaterialTextView
        lateinit var dateText: MaterialTextView
        lateinit var appImage: ImageView
        lateinit var chipVersion: Chip
        lateinit var chipPackage: Chip

        constructor(view: View, activity: Boolean) : this(view) {
            cardView = view.findViewById(R.id.card_item)
            textItem = view.findViewById(R.id.text_item)
            appSize = view.findViewById(R.id.app_size_text)
            appImage = view.findViewById(R.id.application_image)
            chipVersion = view.findViewById(R.id.chip_version)
            if (activity) {
                chipPackage = view.findViewById(R.id.chip_package)
            } else {
                dateText = view.findViewById(R.id.date_text)
            }
        }

    }

    constructor(appList: MutableList<Application>, bitmapList: MutableList<ApplicationBitmap>, isMainActivity: Boolean) : this() {
        this.appList = appList
        this.bitmapList = bitmapList
        this.isMainActivity = isMainActivity
    }

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz liste
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        // Parent parametar, je view grupa za koju će da se zakači list item view kao child view. Parent je RecyclerView.
        // layout sadži referencu na child view (list_item) koji je zakačen na parent view (RecyclerView)
        val activityLayout: Int = if (isMainActivity) {
            R.layout.list_item
        } else {
            R.layout.restore_item
        }
        val layout = LayoutInflater
            .from(parent.context)
            .inflate(activityLayout, parent, false)

        // Vrati ViewHolder
        return AppViewHolder(layout, isMainActivity)
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

        if (isMainActivity) {
            saveBitmap(bitmap.getIcon(), item.getName(), context)
            holder.chipPackage.text = charSequencePackage.toString()
            holder.cardView.setOnClickListener {
                val intent = Intent(context, BackupActivity::class.java)
                intent.putExtra("application", item)
                context.startActivity(intent)
            }
        }
        else {
            holder.dateText.text = item.getDate()

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

    fun updateList(newList: MutableList<Application>, newBitmapList: MutableList<ApplicationBitmap>, activity: Boolean) {
        appList = newList
        bitmapList = newBitmapList
        isMainActivity = activity
        notifyDataSetChanged()
    }
}