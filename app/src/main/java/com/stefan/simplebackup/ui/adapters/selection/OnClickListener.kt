package com.stefan.simplebackup.ui.adapters.selection

import androidx.recyclerview.widget.RecyclerView

interface OnClickListener {
    fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int)

    fun onLongItemViewClick(holder: RecyclerView.ViewHolder, position: Int)
}