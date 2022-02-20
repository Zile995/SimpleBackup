package com.stefan.simplebackup.ui.adapters

interface OnClickListener {
    fun onItemViewClick(holder: AppAdapter.AppViewHolder, position: Int)

    fun onLongItemViewClick(holder: AppAdapter.AppViewHolder, position: Int)
}