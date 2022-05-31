package com.stefan.simplebackup.data.receivers

import android.util.Log
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.withContext

class PackageListenerImpl(application: MainApplication) : PackageListener {
    private val appManager = application.getAppManager
    private val repository = application.getRepository

    // Used to check for changed packages on init
    override suspend fun refreshPackageList() {
        Log.d("ViewModel", "Refreshing the package list")
        withContext(ioDispatcher) {
            appManager.apply {
                if (PreferenceHelper.isDatabaseCreated) {
                    getChangedPackageNames().collect { packageName ->
                        if (doesPackageExists(packageName)) {
                            Log.d("ViewModel", "Adding or updating the $packageName")
                            insertOrUpdatePackage(packageName)
                        } else {
                            Log.d("ViewModel", "Deleting the $packageName")
                            deletePackage(packageName)
                        }
                    }
                }
            }
        }
    }

    override suspend fun insertOrUpdatePackage(packageName: String) {
        appManager.apply {
            repository.insertOrUpdate(build(packageName))
            updateSequenceNumber()
        }
    }

    override suspend fun deletePackage(packageName: String) {
        repository.delete(packageName)
        appManager.updateSequenceNumber()
    }
}