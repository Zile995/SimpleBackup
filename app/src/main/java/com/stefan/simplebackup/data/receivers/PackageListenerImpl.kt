package com.stefan.simplebackup.data.receivers

import android.content.Context
import android.util.Log
import com.stefan.simplebackup.data.local.database.AppDatabase
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.manager.AppManager
import com.stefan.simplebackup.utils.PreferenceHelper
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.withContext

class PackageListenerImpl(context: Context) : PackageListener {

    override val repository = AppRepository(AppDatabase.getInstance(context).appDao())
    private val appManager = AppManager(context)

    // Used to check for changed packages on init
    override suspend fun refreshPackageList() {
        Log.d("PackageListener", "Refreshing the package list")
        withContext(ioDispatcher) {
            appManager.apply {
                if (PreferenceHelper.isDatabaseCreated) {
                    getChangedPackageNames().collect { packageName ->
                        if (doesPackageExists(packageName)) {
                            insertOrUpdatePackage(packageName)
                        } else {
                            deletePackage(packageName)
                        }
                    }
                }
            }
        }
    }

    override suspend fun insertOrUpdatePackage(packageName: String) {
        Log.d("PackageListener", "Adding or updating the $packageName")
        appManager.apply {
            val newApp = build(packageName)
            repository.insertAppData(newApp)
        }
    }

    override suspend fun deletePackage(packageName: String) {
        Log.d("PackageListener", "Deleting the $packageName")
        repository.delete(packageName)
    }

    override suspend fun onActionPackageAdded(packageName: String) {
        insertOrUpdatePackage(packageName)
        appManager.updateSequenceNumber()
    }

    override suspend fun onActionPackageRemoved(packageName: String) {
        deletePackage(packageName)
        appManager.updateSequenceNumber()
    }
}