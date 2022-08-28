package com.stefan.simplebackup.ui.viewmodels

import android.os.Parcelable

interface RecyclerViewStateSaver {
    val savedRecyclerViewState: Parcelable?
    fun saveRecyclerViewState(parcelable: Parcelable)
}