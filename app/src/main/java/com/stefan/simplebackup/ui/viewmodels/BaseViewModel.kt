package com.stefan.simplebackup.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.stefan.simplebackup.data.model.AppData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

const val SELECTION_EXTRA = "SELECTION_LIST"

abstract class BaseViewModel : ViewModel() {
    // Observable spinner properties used for progressbar observing
    private val _spinner = MutableStateFlow(true)
    val spinner = _spinner.asStateFlow()

    // Observable application properties used for list loading
    private val _observableList = MutableStateFlow(mutableListOf<AppData>())
    val observableList = _observableList.asStateFlow()

    fun setSpinning(shouldSpin: Boolean) {
        _spinner.value = shouldSpin
    }

    protected suspend fun loadList(
        shouldControlSpinner: Boolean = true, repositoryList: () -> Flow<MutableList<AppData>>
    ) {
        repositoryList().collect { list ->
            _observableList.value = list
            if (_spinner.value) delay(400)
            if (shouldControlSpinner) _spinner.value = false
        }
    }
}