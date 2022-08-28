package com.stefan.simplebackup.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel(), RecyclerViewStateSaver by RecyclerViewStateSaverImpl() {

    init {
        Log.d("ViewModel", "SearchViewModel created")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "SearchViewModel cleared")
    }


}