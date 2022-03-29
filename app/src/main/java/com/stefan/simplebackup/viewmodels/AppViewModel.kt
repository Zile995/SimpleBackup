package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.broadcasts.PackageListener
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.domain.repository.AppRepository
import com.stefan.simplebackup.ui.adapters.AppAdapter
import com.stefan.simplebackup.ui.adapters.SelectionListener
import com.stefan.simplebackup.utils.backup.BackupWorkerHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppViewModel(application: MainApplication) :
    AndroidViewModel(application), PackageListener, SelectionListener {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val repository: AppRepository = application.getRepository
    private val appManager: AppManager = application.getAppManager

    private val workManager by lazy { WorkManager.getInstance(application) }
    val getWorkManager get() = workManager

    // Observable application properties used for list loading
    private var _allApps = MutableStateFlow(mutableListOf<AppData>())
    val getAllApps: StateFlow<MutableList<AppData>>
        get() = _allApps

    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val isSelected: StateFlow<Boolean> get() = _isSelected
    private val selectionList = mutableListOf<AppData>()

    // Parcelable properties used for saving a RecyclerView layout position
    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = this::state.isInitialized

    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner: StateFlow<Boolean>
        get() = _spinner

    init {
        appManager.printSequence()
        launchDataLoading {
            getAllAppsFromDatabase()
        }
        refreshPackageList()
        Log.d("ViewModel", "AppViewModel created")
    }

    private fun getAllAppsFromDatabase() = repository.getAllApps

    // Loading methods
    private inline fun launchDataLoading(
        crossinline allAppsFromDatabase: () -> Flow<MutableList<AppData>>,
    ) {
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                allAppsFromDatabase().collectLatest { apps ->
                    _allApps.value = apps
                    delay(250)
                    _spinner.value = false
                }
            }.onFailure { throwable ->
                throwable.message?.let { message -> Log.e("ViewModel", message) }
            }
        }
    }

    // Repository methods
    private fun insertApp(app: AppData) = viewModelScope.launch {
        repository.insert(app)
        appManager.updateSequenceNumber()
    }

    private fun deleteApp(packageName: String) = viewModelScope.launch {
        repository.delete(packageName)
        appManager.updateSequenceNumber()
    }

    // Used to check for changed packages on init
    fun refreshPackageList() {
        Log.d("ViewModel", "Refreshing the package list")
        viewModelScope.launch(ioDispatcher) {
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

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }

    // SelectionListener methods - used for RecyclerView selection
    override fun setSelectionMode(selection: Boolean) {
        _isSelected.value = selection
    }

    override fun hasSelectedItems(): Boolean = _isSelected.value

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
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.cardView))
        } else {
            addSelectedItem(item)
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.cardViewSelected))
        }
        if (selectionList.isEmpty()) {
            setSelectionMode(false)
        }
        println("Listener list: ${getSelectedItems().size}: ${getSelectedItems().map { it.name }}")
    }

    // PackageListener methods - Used for database package updates
    override suspend fun addOrUpdatePackage(packageName: String) {
        appManager.apply {
            build(packageName).collect { app ->
                insertApp(app)
            }
        }
    }

    override suspend fun deletePackage(packageName: String) {
        deleteApp(packageName)
    }

    // Batch backup methods
    fun createLocalBackup() {
        viewModelScope.launch(ioDispatcher) {
            val backupWorkerHelper = BackupWorkerHelper(selectionList, workManager)
            backupWorkerHelper.startBackupWorker()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "AppViewModel cleared")
    }
}

class AppViewModelFactory(
    private val application: MainApplication
) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}