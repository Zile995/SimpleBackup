package com.stefan.simplebackup.viewmodels

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.workers.MainWorker
import com.stefan.simplebackup.data.workers.WorkerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val selectionList: IntArray?,
    application: MainApplication
) : AndroidViewModel(application) {

    private val repository: AppRepository = AppRepository(application.database.appDao())
    private val workManager by lazy { WorkManager.getInstance(application) }
    val getWorkManager get() = workManager

    init {
        Log.d("ProgressViewModel", "ProgressViewModel created")
        viewModelScope.launch(Dispatchers.Default) {
            startWorker()
        }
    }

    private fun startWorker() {
        selectionList?.let { appList ->
            val workerHelper = WorkerHelper(appList, workManager)
            workerHelper.beginUniqueWork<MainWorker>()
        }
    }

    suspend fun getCurrentApp(packageName: String) =
        repository.getProgressAppData(packageName)

    override fun onCleared() {
        super.onCleared()
        Log.d("ProgressViewModel", "ProgressViewModel cleared")
    }
}

@Suppress("UNCHECKED_CAST")
class ProgressViewModelFactory(
    private val selectionList: IntArray?,
    private val application: MainApplication
) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            return ProgressViewModel(selectionList, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}