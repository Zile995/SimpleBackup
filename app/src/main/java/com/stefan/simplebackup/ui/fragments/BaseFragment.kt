package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.lang.ref.WeakReference

abstract class BaseFragment<VB : ViewBinding> : Fragment(), RecyclerViewSaver<VB> {
    protected lateinit var bindingReference: WeakReference<VB>

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        bindingReference.saveRecyclerViewState()
    }
}