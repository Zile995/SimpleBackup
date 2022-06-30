package com.stefan.simplebackup.ui.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.ViewModel
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val SELECTION_EXTRA = "SELECTION_LIST"

sealed class BaseViewModel : ViewModel() {
    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner get() = _spinner.asStateFlow()

    // Observable application properties used for list loading
    private var _observableList = MutableStateFlow(listOf<AppData>())
    val observableList = _observableList.asStateFlow()

    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val selectionList = mutableListOf<Int>()
    val isSelected: StateFlow<Boolean> get() = _isSelected
    val setSelectionMode: SelectionModeCallBack =
        { isSelected: Boolean -> _isSelected.value = isSelected }

    // Parcelable properties used for saving a RecyclerView layout position
    private lateinit var state: Parcelable
    val savedRecyclerViewState get() = if (::state.isInitialized) state else null

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        Log.d("ViewModel", "Saving recyclerview state")
        state = parcelable
    }

    protected suspend fun loadList(repositoryList: () -> Flow<List<AppData>>) {
        repositoryList().collect { list ->
            _observableList.value = list
            delay(400)
            _spinner.value = false
        }
    }
}