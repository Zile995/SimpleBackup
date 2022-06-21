package com.stefan.simplebackup.ui.fragments

import androidx.viewbinding.ViewBinding
import java.lang.ref.WeakReference

interface RecyclerViewSaver<VB: ViewBinding> {

    fun WeakReference<VB>.saveRecyclerViewState()

    fun VB.restoreRecyclerViewState()
}