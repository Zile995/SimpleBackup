package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.utils.extensions.launchWithLogging
import kotlinx.coroutines.CoroutineName

class HomeViewModel(
    private val packageListener: PackageListener
) : BaseViewModel() {

    private val repository = packageListener.repository
    suspend fun refreshPackages() = packageListener.refreshPackageList()

    init {
        Log.d("ViewModel", "HomeViewModel created")
        viewModelScope.launchWithLogging(CoroutineName("LoadHomeList")) {
            loadList {
                repository.installedApps
            }
            refreshPackages()
        }
    }
}