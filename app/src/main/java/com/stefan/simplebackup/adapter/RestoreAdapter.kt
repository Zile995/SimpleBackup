package com.stefan.simplebackup.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import kotlin.math.pow

class RestoreAdapter() : RecyclerView.Adapter<RestoreAdapter.RestoreViewHolder>() {

    private var appList = mutableListOf<Application>()
    private var bitmapList = mutableListOf<ApplicationBitmap>()

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

        constructor(
            appList: MutableList<Application>,
            bitmapList: MutableList<ApplicationBitmap>
        ) : this() {
            this.appList = appList
            this.bitmapList = bitmapList
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

            }
        }

    override fun getItemCount() = this.appList.size

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