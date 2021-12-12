package com.stefan.simplebackup.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*

class AppViewModel : ViewModel() {

    private lateinit var applicationList: MutableList<Application>
    private lateinit var result: Deferred<MutableList<Application>>

    init {
        Log.i("AppViewModel", "AppViewModel created!")
        viewModelScope.launch {
            Log.i("AppViewModel", "AppViewModel adding list!")
            result = async { AppInfo.getListFormDatabase() }
            applicationList = result.await()
            Log.i("AppViewModel", "AppViewModel list added!")
            println(applicationList)
        }
    }

    suspend fun getAppList() = result.await()

}