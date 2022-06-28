package com.stefan.simplebackup.ui.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val SELECTION_EXTRA = "SELECTION_LIST"

sealed class BaseViewModel(application: MainApplication) : AndroidViewModel(application) {
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
}