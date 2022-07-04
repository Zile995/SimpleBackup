package com.stefan.simplebackup.ui.adapters.listeners

import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.data.model.AppData

interface SelectionListener<VH : RecyclerView.ViewHolder> {
    val selectedItems: MutableList<Int>

    fun hasSelectedItems(): Boolean

    fun selectMultipleItems(selectedItems: MutableList<Int>)

    fun getSelected(): MutableList<Int>

    fun addSelected(item: Int)

    fun removeSelected(item: Int)

    fun doSelection(holder: VH, item: AppData)

    fun removeSelectedItems() = selectedItems.clear()
}