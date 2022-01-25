package com.stefan.simplebackup.adapter

import com.stefan.simplebackup.data.Application

interface SelectionListener {
    fun setSelection(selection: Boolean)

    fun isSelected(): Boolean

    fun getSelected(): MutableList<Application>

    fun addSelection(app: Application)

    fun removeSelection(app: Application)
}