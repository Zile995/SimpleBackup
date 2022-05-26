package com.stefan.simplebackup.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData

abstract class BaseAppAdapter<VH : BaseViewHolder>(
    override val selectedItems: MutableList<Int>,
    private val onSelectionModeCallback: selectionModeCallBack,
) : ListAdapter<AppData, VH>(diffCallback),
    SelectionListener<VH> by BaseSelectionListenerImpl(
        selectedItems,
        onSelectionModeCallback
    ) {

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<AppData>() {
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