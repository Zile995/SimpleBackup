package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.broadcasts.PackageListener
import com.stefan.simplebackup.data.AppManager
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.domain.repository.AppRepository
import com.stefan.simplebackup.ui.adapters.AppAdapter
import com.stefan.simplebackup.ui.adapters.SelectionListener
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

    // Observable application properties used for list loading
    private var _allApps = MutableStateFlow(mutableListOf<AppData>())
    val getAllApps: StateFlow<MutableList<AppData>>
        get() = _allApps

    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val isSelected: StateFlow<Boolean> get() = _isSelected
    val selectionList = mutableListOf<String>()

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

    override fun setSelectedItems(selectedPackageNames: List<String>) {
        selectionList.clear()
        selectionList.addAll(selectedPackageNames)
    }

    override fun getSelectedItems(): List<String> {
        return selectionList
    }

    override fun addSelectedItem(packageName: String) {
        selectionList.add(packageName)
    }

    override fun removeSelectedItem(packageName: String) {
        selectionList.remove(packageName)
    }

    override fun doSelection(holder: AppAdapter.AppViewHolder, item: AppData) {
        val selectionList = getSelectedItems()
        val context = holder.getContext
        if (selectionList.contains(item.packageName)) {
            removeSelectedItem(item.packageName)
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.cardView))
        } else {
            addSelectedItem(item.packageName)
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.cardViewSelected))
        }
        if (selectionList.isEmpty()) {
            setSelectionMode(false)
        }
        println("Listener list: ${getSelectedItems().size}: ${getSelectedItems()}")
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