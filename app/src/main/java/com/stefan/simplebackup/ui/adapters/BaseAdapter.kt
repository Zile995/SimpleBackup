package com.stefan.simplebackup.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.listeners.SelectionListener
import com.stefan.simplebackup.ui.adapters.viewholders.*

typealias SelectionModeCallBack = (Boolean?) -> Unit

abstract class BaseAdapter(
    override val selectedItems: MutableList<Int>,
    private val onSelectionModeCallback: SelectionModeCallBack,
    val clickListener: () -> OnClickListener
) : ListAdapter<AppData, BaseViewHolder>(DiffCallback()),
    SelectionListener<BaseViewHolder> by BaseSelectionListenerImpl(
        selectedItems,
        onSelectionModeCallback
    ) {

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HomeViewHolder -> holder.bind(item)
            is LocalViewHolder -> holder.bind(item)
            is CloudViewHolder -> holder.bind(item)
            is FavoritesViewHolder -> holder.bind(item)
            is SearchViewHolder -> holder.bind(item)
        }
        if (selectedItems.contains(item.uid)) {
            item.isSelected = true
            holder.setSelected()
        }
    }

    fun clearSelection() {
        currentList.forEachIndexed { index, item ->
            if (item.isSelected) {
                item.isSelected = false
                notifyItemChanged(index)
            }
        }
        removeAllSelectedItems()
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        holder.unsetSelected()
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppData>() {
        override fun areItemsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem.packageName == newItem.packageName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.name == newItem.name &&
                    oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem == newItem
        }
    }
}