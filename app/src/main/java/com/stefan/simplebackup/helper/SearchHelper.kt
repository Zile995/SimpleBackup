package com.stefan.simplebackup.helper

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.MainActivity
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap

internal class SearchHelper private constructor() {

    companion object Search {
        fun search(
            applicationList: MutableList<Application>,
            bitmapList: MutableList<ApplicationBitmap>,
            appAdapter: AppAdapter,
            newText: String?,
            isMainActivity: Boolean
        ) {
            val tempAppList = mutableListOf<Application>()
            val tempBitmapList = mutableListOf<ApplicationBitmap>()
            if (newText.isNullOrEmpty()) {
                appAdapter.updateList(applicationList, bitmapList, isMainActivity)
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
                appAdapter.updateList(tempAppList, tempBitmapList, isMainActivity)
            }
        }
    }
}