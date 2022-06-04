package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel(application: MainApplication) : AndroidViewModel(application) {
    // Selection properties
    private var _isSelected = MutableStateFlow(false)
    val selectionList = mutableListOf<Int>()
    val isSelected: StateFlow<Boolean> get() = _isSelected
    val setSelectionMode: SelectionModeCallBack =
        { isSelected: Boolean -> _isSelected.value = isSelected }

    // Parcelable properties used for saving a RecyclerView layout position
    private lateinit var state: Parcelable
    val restoreRecyclerViewState: Parcelable get() = state
    val isStateInitialized: Boolean get() = this::state.isInitialized

    // Save RecyclerView state
    fun saveRecyclerViewState(parcelable: Parcelable) {
        state = parcelable
    }
}