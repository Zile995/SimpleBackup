package com.stefan.simplebackup.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.utils.backup.BackupUtil
import kotlinx.coroutines.launch

class BackupViewModel(
    private val app: AppData?,
    application: DatabaseApplication
) : AndroidViewModel(application) {

    private val internalStoragePath = application.getInternalStoragePath ?: ""
    private val backupUtil = BackupUtil(app, internalStoragePath)

    init {
        viewModelScope.launch {
            backupUtil.createMainDir()
        }
    }

    fun getApp() = app
}

class BackupViewModelFactory(
    private val app: AppData?,
    private val application: DatabaseApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(app, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}