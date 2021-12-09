package com.stefan.simplebackup.utils

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.activities.MainActivity
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.activities.restore.RestoreActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SearchUtil private constructor() {

    companion object Search {
        fun search(
            applicationList: MutableList<Application>,
            context: Context,
            newText: String?
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val tempAppList = mutableListOf<Application>()
                if (newText.isNullOrEmpty()) {
                    updateList(context, applicationList)
                } else {
                    applicationList.forEach() {
                        if (it.getName().lowercase().contains(newText.lowercase())) {
                            tempAppList.add(it)
                        }
                    }
                    Log.d("string:", newText)
                    updateList(context, tempAppList)
                }
            }
        }

        private suspend fun updateList(
            context: Context,
            applicationList: MutableList<Application>
        ) {
            withContext(Dispatchers.Main) {
                with(context) {
                    when (this) {
                        is MainActivity -> getAdapter().updateList(applicationList)
                        is RestoreActivity -> getAdapter()
                            .updateList(applicationList)
                    }
                }
            }
        }
    }
}