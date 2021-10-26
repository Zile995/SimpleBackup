package com.stefan.simplebackup.utils

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.MainActivity
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import com.stefan.simplebackup.restore.RestoreActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SearchHelper private constructor() {

    companion object Search {
        fun search(
            applicationList: MutableList<Application>,
            bitmapList: MutableList<ApplicationBitmap>,
            context: Context,
            newText: String?
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val tempAppList = mutableListOf<Application>()
                val tempBitmapList = mutableListOf<ApplicationBitmap>()
                if (newText.isNullOrEmpty()) {
                    updateList(context, applicationList, bitmapList)
                } else {
                    applicationList.forEach() {
                        if (it.getName().lowercase().contains(newText.lowercase())) {
                            tempAppList.add(it)
                        }
                    }
                    bitmapList.forEach() {
                        if (it.getName().lowercase().contains(newText.lowercase())) {
                            tempBitmapList.add(it)
                        }
                    }
                    Log.d("string:", newText)
                    updateList(context, tempAppList, tempBitmapList)
                }
            }
        }

        private suspend fun updateList(
            context: Context,
            applicationList: MutableList<Application>,
            bitmapList: MutableList<ApplicationBitmap>
        ) {
            withContext(Dispatchers.Main) {
                with(context) {
                    when (this) {
                        is MainActivity -> getAdapter().updateList(applicationList, bitmapList)
                        is RestoreActivity -> getAdapter()
                            .updateList(applicationList, bitmapList, context)
                    }
                }
            }
        }
    }
}