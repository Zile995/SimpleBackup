package com.stefan.simplebackup.ui.fragments

import androidx.viewbinding.ViewBinding

interface RecyclerViewSaver<T: ViewBinding> {
    fun T.saveRecyclerViewState()

    fun T.restoreRecyclerViewState()
}