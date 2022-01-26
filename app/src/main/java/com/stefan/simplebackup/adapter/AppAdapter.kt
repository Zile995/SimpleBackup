package com.stefan.simplebackup.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.ui.activities.BackupActivity
import com.stefan.simplebackup.utils.FileUtil

class AppAdapter(private val selectionListener: SelectionListener) :
    ListAdapter<Application, AppAdapter.AppViewHolder>(AppDiffCallBack()), Filterable {

    private var appList = mutableListOf<Application>()

    class AppViewHolder private constructor(private val view: View) : RecyclerView.ViewHolder(view) {
        private var bitmap: Bitmap? = null
        private val cardView: MaterialCardView = view.findViewById(R.id.card_item)
        private val textItem: MaterialTextView = view.findViewById(R.id.text_item)
        private val appSize: MaterialTextView = view.findViewById(R.id.app_size_text)
        private val appImage: ImageView = view.findViewById(R.id.application_image)
        private val chipVersion: Chip = view.findViewById(R.id.chip_version)
        private val chipPackage: Chip = view.findViewById(R.id.chip_package)

        val getBitmap get() = bitmap
        val getCardView get() = cardView
        val getContext: Context get() = view.context

        fun bind(item: Application) {
            bitmap = item.getBitmapFromArray()
            val packageNameSequence: CharSequence = item.getPackageName()
            val versionNameSequence: CharSequence = "v" + item.getVersionName()

            textItem.text = item.getName()
            appImage.setImageBitmap(bitmap)
            chipVersion.text = versionNameSequence.toString()
            appSize.text = FileUtil.transformBytes(item.getApkSize())
            chipPackage.text = packageNameSequence.toString()
        }

        companion object {
            fun from(parent: ViewGroup): AppViewHolder {
                val layout = LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.list_item, parent, false)

                // Vrati ViewHolder
                return AppViewHolder(layout)
            }
        }
    }

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz list item-a
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        // Parent parametar, je view grupa za koju će da se zakači list item view kao child view. Parent je RecyclerView.
        // layout sadži referencu na child view (list_item) koji je zakačen na parent view (RecyclerView)
        return AppViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        if (selectionListener.getSelected().contains(item)) {
            holder.getCardView.setCardBackgroundColor(holder.getContext.getColor(R.color.selected_card))
        }

        holder.getCardView.setOnLongClickListener {
            selectionListener.setSelection(true)
            doSelection(holder, position)
            true
        }

        holder.getCardView.setOnClickListener {
            if (selectionListener.isSelected()) {
                doSelection(holder, position)
            } else {
                val context = holder.getContext
                val bitmap = holder.getBitmap
                /** Zato što parcelable ima Binder IPC ograničenje, postavi prazan byte niz za prenos
                 *  i sačuvaj bitmap array u MODE_PRIVATE, odnosno u data/files folder naše aplikacije
                 */
                if (bitmap != null && bitmap.allocationByteCount > 500000) {
                    FileUtil.saveBitmap(bitmap, item.getName(), context)
                    item.setBitmap(byteArrayOf())
                    println("Bitmap = ${item.getBitmapFromArray()}")
                }
                val intent = Intent(context, BackupActivity::class.java)
                intent.putExtra("application", item)
                context.startActivity(intent)
            }
        }
    }

    private fun doSelection(holder: AppViewHolder, position: Int) {
        val context = holder.getContext
        if (selectionListener.getSelected().contains(appList[position])) {
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.card))
            selectionListener.removeSelection(appList[position])
        } else {
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.selected_card))
            selectionListener.addSelection(appList[position])
        }
        if (selectionListener.getSelected().isEmpty()) {
            selectionListener.setSelection(false)
        }
        println("Listener list: ${selectionListener.getSelected().size}")
    }

    fun setData(list: MutableList<Application>) {
        appList = list
        submitList(appList)
    }

    override fun getFilter(): Filter {
        return appFilter
    }

    private val appFilter = object : Filter() {
        override fun performFiltering(sequence: CharSequence?): FilterResults {
            val filteredList = mutableListOf<Application>()
            if (sequence.isNullOrBlank()) {
                filteredList.addAll(appList)
            } else {
                appList.forEach {
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
            submitList(results?.values as MutableList<Application>)
        }
    }

}

class AppDiffCallBack : DiffUtil.ItemCallback<Application>() {
    override fun areItemsTheSame(oldItem: Application, newItem: Application): Boolean {
        return oldItem.getName() == newItem.getName() &&
                oldItem.getPackageName() == newItem.getPackageName() &&
                oldItem.getVersionName() == newItem.getVersionName()
    }

    override fun areContentsTheSame(oldItem: Application, newItem: Application): Boolean {
        return oldItem == newItem
    }
}