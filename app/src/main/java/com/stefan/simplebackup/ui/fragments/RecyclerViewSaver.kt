package com.stefan.simplebackup.ui.fragments

import androidx.viewbinding.ViewBinding

interface RecyclerViewSaver<VB : ViewBinding> {

    fun VB.saveRecyclerViewState()

    fun VB.restoreRecyclerViewState()
}