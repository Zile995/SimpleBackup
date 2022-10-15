package com.stefan.simplebackup.ui.viewmodels

import android.os.Parcelable
import android.util.Log

class RecyclerViewStateSaverImpl : RecyclerViewStateSaver {
    // Parcelable properties used for saving a RecyclerView layout position
    private lateinit var state: Parcelable

    override val savedRecyclerViewState: Parcelable?
        get() = if (::state.isInitialized) state else null

    // Save RecyclerView state
    override fun saveRecyclerViewState(parcelable: Parcelable) {
        Log.d("RecyclerViewStateSaver", "Saving recyclerview state")
        state = parcelable
    }
}