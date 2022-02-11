package com.stefan.simplebackup.adapter

import android.view.View
import com.stefan.simplebackup.data.AppData

interface OnClickListener {
    fun onItemViewClick(holder: AppAdapter.AppViewHolder, item: AppData)

    fun onLongItemViewClick(holder: AppAdapter.AppViewHolder, item: AppData)
}