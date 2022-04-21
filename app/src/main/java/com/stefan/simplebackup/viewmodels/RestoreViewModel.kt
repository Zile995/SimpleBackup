package com.stefan.simplebackup.viewmodels

import android.app.Application
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkManager
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.RestoreAdapter
import com.stefan.simplebackup.ui.adapters.SelectionListener
import com.stefan.simplebackup.utils.backup.WorkerHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RestoreViewModel(application: Application) : AndroidViewModel(application),
    SelectionListener {

    private val workManager by lazy { WorkManager.getInstance(application) }
    val getWorkManager get() = workManager

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
        Log.d("ViewModel", "RestoreViewModel created")
    }

    fun startRestoreWorker() {
        viewModelScope.launch(ioDispatcher) {
            val workerHelper = WorkerHelper(selectionList.toTypedArray(), workManager)
            workerHelper.startWorker(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "RestoreViewModel cleared")
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

    override fun doSelection(holder: RecyclerView.ViewHolder, item: AppData) {
        val selectionList = getSelectedItems()
        val context = (holder as RestoreAdapter.RestoreViewHolder).getContext
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

}

class RestoreViewModelFactory(private val application: Application) :
    ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        if (modelClass.isAssignableFrom(RestoreViewModel::class.java)) {
            return RestoreViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}