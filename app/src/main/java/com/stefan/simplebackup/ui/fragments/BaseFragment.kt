package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.*
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VB : ViewBinding> : Fragment(), RecyclerViewSaver<VB>,
    ViewReferenceCleaner {
    protected val binding by viewBinding()
    protected val mainViewModel: MainViewModel by activityViewModels()

    private var _adapter: BaseAdapter? = null
    protected val adapter: BaseAdapter get() = _adapter!!

    protected var shouldEnableOnLongClick = true
    private var _mainRecyclerView: MainRecyclerView? = null

    private val onClickListener by lazy {
        object : OnClickListener {
            override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = adapter.currentList[position]
                if (adapter.hasSelectedItems()) {
                    adapter.doSelection(holder as BaseViewHolder, item)
                } else {
                    launchOnViewLifecycle {
                        item.passToActivity<AppDetailActivity>(activity)
                    }
                }
            }

            override fun onLongItemViewClick(
                holder: RecyclerView.ViewHolder,
                position: Int
            ): Boolean {
                if (!shouldEnableOnLongClick) return false
                val item = adapter.currentList[position]
                mainViewModel.setSelectionMode(true)
                adapter.doSelection(holder as BaseViewHolder, item)
                return true
            }
        }
    }

    abstract fun MainRecyclerView.onCreateAdapter(onClickListener: OnClickListener): BaseAdapter

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
            adapter = onCreateAdapter(onClickListener)
            _adapter = this.adapter as BaseAdapter
            setHasFixedSize(true)
        }
    }

    private fun initObservers() {
        launchOnViewLifecycle {
            repeatOnViewLifecycle(Lifecycle.State.RESUMED) {
                mainViewModel.isSelected.collect { isSelected ->
                    if (!isSelected) clearSelection()
                    enableRecyclerViewScrolling(!isSelected)
                }
            }
        }
    }

    fun stopScrolling() {
        _mainRecyclerView?.suppressLayout(true)
        _mainRecyclerView?.suppressLayout(false)
    }

    private fun enableRecyclerViewScrolling(shouldEnable: Boolean) {
        _mainRecyclerView?.isNestedScrollingEnabled = shouldEnable
    }

    fun deleteSelectedItem() {
        context?.deletePackage(mainViewModel.selectionList.first())
        mainViewModel.setSelectionMode(false)
    }

    fun deleteLocalBackups() {
        mainViewModel.deleteSelectedBackups()
    }


    fun selectAllItems() {
        adapter.selectAllItems()
        Snackbar.make(
            binding.root,
            "Selected ${mainViewModel.selectionList.size} apps",
            1250
        ).show()
    }

    private fun clearSelection() = adapter.clearSelection()

    fun shouldMoveFragmentUp() = _mainRecyclerView?.shouldMoveAtLastCompletelyVisibleItem() ?: false

    fun fixRecyclerViewScrollPosition() {
        if (shouldMoveFragmentUp()) _mainRecyclerView?.slowlyScrollToLastItem()
    }

    override fun onCleanUp() {
        binding.saveRecyclerViewState()
        _adapter = null
        _mainRecyclerView?.adapter = null
        _mainRecyclerView = null
    }
}