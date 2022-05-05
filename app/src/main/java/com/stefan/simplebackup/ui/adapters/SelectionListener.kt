package com.stefan.simplebackup.ui.adapters

import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.data.model.AppData

interface SelectionListener<VH: RecyclerView.ViewHolder> {
    val selectedPackageNames: MutableList<String>

    fun hasSelectedItems(): Boolean

    fun selectMultipleItems(selectedPackageNames: List<String>)

    fun getSelectedItems(): List<String>

    fun addSelectedItem(packageName: String)

    fun removeSelectedItem(packageName: String)

    fun doSelection(holder: VH, item: AppData)
}