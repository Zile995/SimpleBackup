package com.stefan.simplebackup.viewmodel

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.*
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stefan.simplebackup.R
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.adapter.SelectionListener
import com.stefan.simplebackup.broadcasts.PackageListener
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.database.AppRepository
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.utils.backup.BackupHelper
import com.stefan.simplebackup.workers.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: DatabaseApplication) :
    ViewModel(), PackageListener, SelectionListener {
    private val repository: AppRepository = application.getRepository
    private val appManager: AppManager = application.getAppManager
    private val mainDirPath by lazy { application.getMainBackupDirPath }

    private val workManager = WorkManager.getInstance(application)

    private lateinit var allApps: LiveData<MutableList<AppData>>
    val getAllApps: LiveData<MutableList<AppData>>
        get() = allApps

    private var _isSelected = MutableLiveData(false)
    val isSelected: LiveData<Boolean> get() = _isSelected
    private val selectionList = mutableListOf<AppData>()

    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = ::state.isInitialized

    private var _spinner = MutableLiveData(true)
    val spinner: LiveData<Boolean>
        get() = _spinner

    init {
        appManager.printSequence()
        launchListLoading { getAllAppsFromRepository() }
        Log.d("ViewModel", "AppViewModel created")
    }

    // Loading methods
    private fun launchListLoading(block: () -> LiveData<MutableList<AppData>>) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                allApps = block()
            }.onSuccess {
                refreshPackageList()
                checkIfLoaded()
                _spinner.postValue(false)
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("ViewModel", message) }
            }
        }
    }

    // Repository methods
    private fun getAllAppsFromRepository(): LiveData<MutableList<AppData>> {
        return repository.getAllApps
    }

    private fun insertApp(app: AppData) = viewModelScope.launch {
        repository.insert(app)
        appManager.updateSequenceNumber()
    }

    private fun deleteApp(packageName: String) = viewModelScope.launch {
        repository.delete(packageName)
        appManager.updateSequenceNumber()
    }

    // Used to check for changed packages on init
    suspend fun refreshPackageList() {
        Log.d("ViewModel", "Refreshing the package list")
        viewModelScope.launch(Dispatchers.IO) {
            appManager.apply {
                getChangedPackageNames().collect { packageName ->
                    if (doesPackageExists(packageName)) {
                        Log.d("ViewModel", "Adding or updating the $packageName")
                        addOrUpdatePackage(packageName)
                    } else {
                        Log.d("ViewModel", "Deleting the $packageName")
                        deletePackage(packageName)
                    }
                }
            }
        }
    }

    // Wait for database list
    private suspend fun checkIfLoaded() {
        withContext(Dispatchers.IO) {
            val numberOfInstalled = appManager.getNumberOfInstalled()
            while (true) {
                val numberOfStored = getAllApps.value?.size ?: 0
                if (numberOfStored == numberOfInstalled)
                    break
            }
        }
    }

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    // SelectionListener methods - used for RecyclerView selection
    override fun setSelectionMode(selection: Boolean) {
        _isSelected.postValue(selection)
    }

    override fun hasSelectedItems(): Boolean = _isSelected.value ?: false

    override fun setSelectedItems(selectedList: MutableList<AppData>) {
        selectionList.clear()
        selectionList.addAll(selectedList)
    }

    override fun getSelectedItems(): MutableList<AppData> {
        return selectionList
    }

    override fun addSelectedItem(app: AppData) {
        selectionList.add(app)
    }

    override fun removeSelectedItem(app: AppData) {
        selectionList.remove(app)
    }

    override fun doSelection(holder: AppAdapter.AppViewHolder, item: AppData) {
        val selectionList = getSelectedItems()
        val context = holder.getContext
        if (selectionList.contains(item)) {
            removeSelectedItem(item)
            holder.getCardView.toggle()
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.cardView))
        } else {
            addSelectedItem(item)
            holder.getCardView.toggle()
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.cardViewSelected))
        }
        if (selectionList.isEmpty()) {
            setSelectionMode(false)
        }
        println("Listener list: ${getSelectedItems().size}")
    }

    // PackageListener methods - Used for database package updates
    override suspend fun addOrUpdatePackage(packageName: String) {
        viewModelScope.launch {
            appManager.apply {
                build(packageName).collect { app ->
                    insertApp(app)
                }
            }
        }
    }

    override suspend fun deletePackage(packageName: String) {
        deleteApp(packageName)
    }

    fun createLocalBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val backupHelper = BackupHelper(selectionList, mainDirPath)
            val backupRequest = OneTimeWorkRequestBuilder<BackupWorker>()
                .setInputData(backupHelper.createInputData())
                .build()
            workManager.enqueue(backupRequest)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "AppViewModel cleared")
    }
}

class AppViewModelFactory(
    private val application: DatabaseApplication
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}