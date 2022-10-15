package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stefan.simplebackup.MainApplication

class SearchViewModel : ViewModel(), RecyclerViewStateSaver by RecyclerViewStateSaverImpl() {
    init {
        Log.d("ViewModel", "SearchViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "SearchViewModel cleared")
    }
}

class SearchViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel() as T
        }
        throw IllegalArgumentException("Unable to construct SearchViewModel")
    }
}