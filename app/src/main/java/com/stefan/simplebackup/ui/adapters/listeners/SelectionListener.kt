package com.stefan.simplebackup.ui.adapters.listeners

import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.data.model.AppData

interface SelectionListener<VH : RecyclerView.ViewHolder> {
    val selectedItems: MutableList<String>

    fun hasSelectedItems(): Boolean

    fun selectMultipleItems(selectedItems: List<String>)

    fun getSelected(): List<String>

    fun addSelected(item: String)

    fun removeSelected(item: String)

    fun doSelection(holder: VH, item: AppData)

    fun removeAllSelectedItems()
}