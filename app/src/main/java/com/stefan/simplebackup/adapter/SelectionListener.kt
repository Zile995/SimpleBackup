package com.stefan.simplebackup.adapter

import com.stefan.simplebackup.data.AppData

interface SelectionListener {
    fun setSelection(selection: Boolean)

    fun isSelected(): Boolean

    fun setSelected(selectedList: MutableList<AppData>)

    fun getSelected(): MutableList<AppData>

    fun addSelection(app: AppData)

    fun removeSelection(app: AppData)
}