package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.launchOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.onMainActivityCallback
import com.stefan.simplebackup.utils.extensions.repeatOnViewLifecycle
import com.stefan.simplebackup.utils.extensions.viewBinding
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VB : ViewBinding> : Fragment(), RecyclerViewSaver<VB>,
    ViewReferenceCleaner {
    protected val binding by viewBinding()
    protected val mainViewModel: MainViewModel by activityViewModels()
    private var _mainRecyclerView: MainRecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getRecyclerView()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        onMainActivityCallback {
            _mainRecyclerView?.controlFloatingButton()
        }
    }

    override fun onCleanUp() {
        binding.saveRecyclerViewState()
        _mainRecyclerView = null
    }

    fun selectAllItems() {
        _mainRecyclerView?.apply {
            val currentAdapter = adapter as BaseAdapter
            currentAdapter.selectAllItems()
        }
    }

    fun clearSelection() {
        _mainRecyclerView?.apply {
            val currentAdapter = adapter as BaseAdapter
            currentAdapter.clearSelection()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getRecyclerView() {
        val vbClass =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        val declaredFields = vbClass.declaredFields
        val mainRecyclerViewField = declaredFields.first { declaredField ->
            declaredField.type == MainRecyclerView::class.java
        }
        mainRecyclerViewField.apply {
            _mainRecyclerView = vbClass.getDeclaredField(name).get(binding) as MainRecyclerView
        }
    }

    fun shouldMoveFragmentUp() = _mainRecyclerView?.shouldMoveAtLastCompletelyVisibleItem() ?: false

    private fun initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.RESUMED) {
                mainViewModel.isSelected.collect { isSelected ->
                    if (!isSelected) clearSelection()
                    onMainActivityCallback {
                        _mainRecyclerView?.isNestedScrollingEnabled = !isSelected
                    }
                }
            }
        }
    }
}