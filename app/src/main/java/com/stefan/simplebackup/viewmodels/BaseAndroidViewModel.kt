package com.stefan.simplebackup.viewmodels

import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.ui.adapters.selectionModeCallBack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseAndroidViewModel(application: MainApplication) : AndroidViewModel(application) {
    // Selection properties
    protected val selectionList = mutableListOf<Int>()
    private var _isSelected = MutableStateFlow(false)
    protected val isSelected: StateFlow<Boolean> get() = _isSelected
    protected val setSelectionMode: selectionModeCallBack =
        { isSelected: Boolean -> _isSelected.value = isSelected }

    // Parcelable properties used for saving a RecyclerView layout position
    protected lateinit var state: Parcelable
    protected val restoreRecyclerViewState: Parcelable get() = state
    protected val isStateInitialized: Boolean get() = this::state.isInitialized
}