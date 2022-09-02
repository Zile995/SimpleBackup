package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.*
import java.lang.reflect.ParameterizedType


abstract class BaseFragment<VB : ViewBinding> : Fragment(), RecyclerViewSaver<VB>,
    ViewReferenceCleaner {
    protected val binding by viewBinding()
    protected val mainViewModel: MainViewModel by activityViewModels()
    private var _mainRecyclerView: MainRecyclerView? = null

    abstract fun MainRecyclerView.setMainAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setMainRecyclerView()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        onMainActivityCallback { _mainRecyclerView?.controlFloatingButton() }
    }

    override fun onCleanUp() {
        binding.saveRecyclerViewState()
        _mainRecyclerView?.adapter = null
        _mainRecyclerView = null
    }

    fun deleteSelectedItems() {
        _mainRecyclerView?.apply {
            val currentAdapter = adapter as BaseAdapter
            currentAdapter.selectedItems.forEach { packageName ->
                requireContext().deletePackage(packageName)
            }
            mainViewModel.setSelectionMode(false)
        }
    }

    fun selectAllItems() {
        _mainRecyclerView?.apply {
            itemAnimation = false
            val currentAdapter = adapter as BaseAdapter
            currentAdapter.selectAllItems()
            itemAnimation = true
            Snackbar.make(
                binding.root,
                "Selected ${mainViewModel.selectionList.size} apps",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearSelection() {
        _mainRecyclerView?.apply {
            itemAnimation = false
            val currentAdapter = adapter as BaseAdapter
            currentAdapter.clearSelection()
            itemAnimation = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun setMainRecyclerView() {
        val vbClass =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        val declaredFields = vbClass.declaredFields
        val mainRecyclerViewField = declaredFields.first { declaredField ->
            declaredField.type == MainRecyclerView::class.java
        }
        mainRecyclerViewField.apply {
            _mainRecyclerView = vbClass.getDeclaredField(name).get(binding) as MainRecyclerView
        }
        _mainRecyclerView?.apply {
            setMainAdapter()
            setHasFixedSize(true)
        }
    }

    fun shouldMoveFragmentUp() = _mainRecyclerView?.shouldMoveAtLastCompletelyVisibleItem() ?: false

    fun fixRecyclerViewScrollPosition() {
        if (shouldMoveFragmentUp()) _mainRecyclerView?.slowlyScrollToLastItem()
    }

    private fun initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.RESUMED) {
                mainViewModel.isSelected.collect { isSelected ->
                    if (!isSelected) clearSelection()
                    _mainRecyclerView?.isNestedScrollingEnabled = !isSelected
                }
            }
        }
    }
}