package com.stefan.simplebackup.helper

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.MainActivity
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import com.stefan.simplebackup.restore.RestoreActivity

internal class SearchHelper private constructor() {

    companion object Search {
        fun search(
            applicationList: MutableList<Application>,
            bitmapList: MutableList<ApplicationBitmap>,
            context: Context,
            newText: String?
        ) {
            val tempAppList = mutableListOf<Application>()
            val tempBitmapList = mutableListOf<ApplicationBitmap>()
            if (newText.isNullOrEmpty()) {
                updateList(context, applicationList, bitmapList)
            } else {
                applicationList.forEach() {
                    if (it.getName().lowercase().startsWith(newText.lowercase())) {
                        tempAppList.add(it)
                    }
                }
                bitmapList.forEach() {
                    if (it.getName().lowercase().startsWith(newText.lowercase())) {
                        tempBitmapList.add(it)
                    }
                }
                Log.d("string:", newText)
                updateList(context, tempAppList, tempBitmapList)
            }
        }

        private fun updateList(
            context: Context,
            applicationList: MutableList<Application>,
            bitmapList: MutableList<ApplicationBitmap>
        ) {
            when (context) {
                is MainActivity -> context.getAdapter().updateList(applicationList, bitmapList)
                is RestoreActivity -> context.getAdapter()
                    .updateList(applicationList, bitmapList, context)
            }
        }
    }
}