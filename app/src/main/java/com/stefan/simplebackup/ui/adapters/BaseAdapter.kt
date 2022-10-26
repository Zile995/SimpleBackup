package com.stefan.simplebackup.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.listeners.SelectionListener
import com.stefan.simplebackup.ui.adapters.viewholders.*

typealias SelectionModeCallBack = (Boolean) -> Unit

abstract class BaseAdapter(
    override val selectedItems: MutableList<String>,
    private val onSelectionModeCallback: SelectionModeCallBack,
    val clickListener: OnClickListener
) : ListAdapter<AppData, BaseViewHolder>(DiffCallback()),
    SelectionListener<BaseViewHolder> by BaseSelectionListenerImpl(
        selectedItems,
        onSelectionModeCallback
    ) {

    @Synchronized
    override fun submitList(list: List<AppData>?) {
        list?.let { newList ->
            if (newList.size < currentList.size && !BaseSelectionListenerImpl.selectionFinished) {
                val deletedPackageNames = currentList.asSequence().minus(newList.toSet()).map { it.packageName }
                deletedPackageNames.forEach { packageName ->
                    if (selectedItems.contains(packageName)) {
                        removeSelected(packageName)
                    }
                }
            }
        }
        super.submitList(list)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HomeViewHolder -> holder.bind(item)
            is LocalViewHolder -> holder.bind(item)
            is SearchViewHolder -> holder.bind(item)
            is FavoritesViewHolder -> holder.bind(item)
        }
        holder.setSelectedItem(item)
    }

    fun getCurrentlySelectedItems() = currentList.filter { app -> selectedItems.contains(app.packageName) }

    private fun BaseViewHolder.setSelectedItem(item: AppData) {
        if (selectedItems.contains(item.packageName)) {
            item.isSelected = true
            setSelected()
        } else {
            item.isSelected = false
            unsetSelected()
        }
    }

    private fun List<AppData>.getSelectedIndexedValues() =
        asSequence().withIndex().filter { indexedValue ->
            selectedItems.contains(indexedValue.value.packageName)
        }

    fun selectAllItems() {
        val transformedList = currentList.map {
            it.packageName
        }
        selectMultipleItems(transformedList)
        currentList.forEachIndexed { index, item ->
            if (!item.isSelected) {
                item.isSelected = true
                notifyItemChanged(index)
            }
        }
    }

    fun clearSelection() {
        if (!hasSelectedItems()) return
        currentList.getSelectedIndexedValues().forEach { indexedValue ->
            if (indexedValue.value.isSelected) {
                indexedValue.value.isSelected = false
                notifyItemChanged(indexedValue.index)
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