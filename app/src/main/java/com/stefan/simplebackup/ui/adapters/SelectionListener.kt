package com.stefan.simplebackup.ui.adapters

import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.data.model.AppData

interface SelectionListener {
    fun setSelectionMode(selection: Boolean)

    fun hasSelectedItems(): Boolean

    fun setSelectedItems(selectedPackageNames: List<String>)

    fun getSelectedItems(): List<String>

    fun addSelectedItem(packageName: String)

    fun removeSelectedItem(packageName: String)

    fun doSelection(holder: RecyclerView.ViewHolder, item: AppData)
}