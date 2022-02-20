package com.stefan.simplebackup.ui.adapters

import com.stefan.simplebackup.data.AppData

interface SelectionListener {
    fun setSelectionMode(selection: Boolean)

    fun hasSelectedItems(): Boolean

    fun setSelectedItems(selectedList: MutableList<AppData>)

    fun getSelectedItems(): MutableList<AppData>

    fun addSelectedItem(app: AppData)

    fun removeSelectedItem(app: AppData)

    fun doSelection(holder: AppAdapter.AppViewHolder, item: AppData)
}