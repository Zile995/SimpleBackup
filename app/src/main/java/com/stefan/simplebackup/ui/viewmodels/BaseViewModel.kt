package com.stefan.simplebackup.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

const val SELECTION_EXTRA = "SELECTION_LIST"

sealed class BaseViewModel(private val shouldControlSpinner: Boolean = true) : ViewModel(),
    RecyclerViewStateSaver by RecyclerViewStateSaverImpl() {
    // Observable spinner properties used for progressbar observing
    private var _spinner = MutableStateFlow(true)
    val spinner get() = _spinner.asStateFlow()

    // Observable application properties used for list loading
    private var _observableList = MutableStateFlow(listOf<AppData>())
    val observableList get() = _observableList.asStateFlow()

    fun setSpinning(shouldSpin: Boolean) {
        _spinner.value = shouldSpin
    }

    protected suspend fun loadList(repositoryList: () -> Flow<List<AppData>>) {
        repositoryList().collect { list ->
            _observableList.value = list
            delay(400)
            if (shouldControlSpinner) _spinner.value = false
        }
    }
}