package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData

class AppAdapter(
    private val selectedPackageNames: MutableList<String>,
    private val clickListener: OnClickListener,
    private val onSelectionModeCallback: (Boolean) -> Unit
) :
    ListAdapter<AppData, AppViewHolder>(AppDiffCallBack),
    SelectionListener {

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz list item-a
     * - Parent parametar, je view grupa za koju će da se zakači list item view kao child view. Parent je RecyclerView.
     * - Layout sadži referencu na child view (list_item) koji je zakačen na parent view (RecyclerView)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder.getViewHolder(parent, clickListener)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        if (selectedPackageNames.contains(item.packageName)) {
            holder.cardView.apply {
                setCardBackgroundColor(
                    holder.context.getColor(R.color.cardViewSelected)
                )
            }
        }
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        holder.cardView.apply {
            setCardBackgroundColor(
                context.getColor(R.color.cardView)
            )
        }
        super.onViewRecycled(holder)
    }

    override fun hasSelectedItems(): Boolean {
        return selectedPackageNames.isNotEmpty()
    }

    // SelectionListener methods - used for RecyclerView selection
    override fun setSelectedItems(selectedPackageNames: List<String>) {
        this.selectedPackageNames.clear()
        this.selectedPackageNames.addAll(selectedPackageNames)
    }

    override fun getSelectedItems(): List<String> {
        return selectedPackageNames
    }

    override fun addSelectedItem(packageName: String) {
        selectedPackageNames.add(packageName)
    }

    override fun removeSelectedItem(packageName: String) {
        selectedPackageNames.remove(packageName)
    }

    override fun doSelection(holder: RecyclerView.ViewHolder, item: AppData) {
        val context = (holder as AppViewHolder).context
        if (selectedPackageNames.contains(item.packageName)) {
            removeSelectedItem(item.packageName)
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.cardView))
        } else {
            addSelectedItem(item.packageName)
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.cardViewSelected))
        }
        if (selectedPackageNames.isEmpty()) {
            onSelectionModeCallback(false)
        }
        println("Listener list: ${getSelectedItems().size}: ${getSelectedItems()}")
    }

    companion object {
        val AppDiffCallBack = object : DiffUtil.ItemCallback<AppData>() {
            override fun areItemsTheSame(oldItem: AppData, newItem: AppData): Boolean {
                return oldItem.packageName == newItem.packageName &&
                        oldItem.versionName == newItem.versionName &&
                        oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: AppData, newItem: AppData): Boolean {
                return oldItem == newItem
            }
        }
    }
}

