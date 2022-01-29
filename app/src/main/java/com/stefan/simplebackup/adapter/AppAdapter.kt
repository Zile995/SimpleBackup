package com.stefan.simplebackup.adapter

import android.annotation.SuppressLint
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
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.ui.activities.BackupActivity
import com.stefan.simplebackup.utils.FileUtil

class AppAdapter(private val selectionListener: SelectionListener) :
    ListAdapter<AppData, AppAdapter.AppViewHolder>(AppDiffCallBack()), Filterable {

    private var appList = mutableListOf<AppData>()

    class AppViewHolder private constructor(private val view: View) : RecyclerView.ViewHolder(view) {
        private var bitmap: Bitmap? = null
        private val cardView: MaterialCardView = view.findViewById(R.id.card_item)
        private val appImage: ImageView = view.findViewById(R.id.application_image)
        private val appName: MaterialTextView = view.findViewById(R.id.application_name)
        private val versionName: MaterialTextView = view.findViewById(R.id.version_name)
        private val packageName: MaterialTextView = view.findViewById(R.id.package_name)
        private val apkSize: Chip = view.findViewById(R.id.apk_size)

        val getBitmap get() = bitmap
        val getCardView get() = cardView
        val getContext: Context get() = view.context

        fun bind(item: AppData) {
            bitmap = item.getBitmapFromArray()

            appImage.setImageBitmap(bitmap)
            appName.text = item.getName()
            versionName.text = item.getVersionName()
            packageName.text = item.getPackageName()
            apkSize.text = FileUtil.transformBytes(item.getApkSize())
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
        val selectionList = selectionListener.getSelected()
        val context = holder.getCardView.context
        if (selectionList.contains(appList[position])) {
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.card))
            selectionListener.removeSelection(appList[position])
        } else {
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.selected_card))
            selectionListener.addSelection(appList[position])
        }
        if (selectionList.isEmpty()) {
            selectionListener.setSelection(false)
        }
        println("Listener list: ${selectionListener.getSelected().size}")
    }

    fun setData(list: MutableList<AppData>) {
        appList = list
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

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(sequence: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            submitList(results?.values as MutableList<AppData>)
            notifyDataSetChanged()
        }
    }

}

class AppDiffCallBack : DiffUtil.ItemCallback<AppData>() {
    override fun areItemsTheSame(oldItem: AppData, newItem: AppData): Boolean {
        return oldItem.getName() == newItem.getName() &&
                oldItem.getPackageName() == newItem.getPackageName() &&
                oldItem.getVersionName() == newItem.getVersionName()
    }

    override fun areContentsTheSame(oldItem: AppData, newItem: AppData): Boolean {
        return oldItem == newItem
    }
}