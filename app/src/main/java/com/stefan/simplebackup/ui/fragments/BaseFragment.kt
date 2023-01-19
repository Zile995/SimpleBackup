package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import com.stefan.simplebackup.R
import com.stefan.simplebackup.ui.activities.DetailActivity
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.listeners.BaseSelectionListenerImpl.Companion.inSelection
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.viewmodels.MainViewModel
import com.stefan.simplebackup.ui.views.MainRecyclerView
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.launch
import java.lang.reflect.ParameterizedType

abstract class BaseFragment<VB : ViewBinding> : Fragment(), RecyclerViewSaver<VB>,
    ViewReferenceCleaner, ButtonSelectionAction {

    protected val binding by viewBinding()
    protected val mainViewModel: MainViewModel by activityViewModels()

    private var _adapter: BaseAdapter? = null
    protected val adapter: BaseAdapter get() = _adapter!!
    private var _mainRecyclerView: MainRecyclerView? = null

    protected var shouldEnableOnLongClick = true

    private val onClickListener by lazy {
        object : OnClickListener {
            override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = adapter.currentList[position]
                if (adapter.hasSelectedItems()) {
                    adapter.doSelection(holder as BaseViewHolder, item)
                } else {
                    launchOnViewLifecycle { item.passToActivity<DetailActivity>(requireContext()) }
                }
            }

            override fun onLongItemViewClick(
                holder: RecyclerView.ViewHolder, position: Int
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
        controlButton()
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
            launch {
                repeatOnStarted {
                    mainViewModel.isSelected.collect { isSelected ->
                        enableRecyclerViewNestedScrolling(!isSelected)
                    }
                }
            }
            repeatOnCreated {
                mainViewModel.isSelected.collect { isSelected ->
                    if (isVisible) {
                        if (!isSelected) adapter.clearSelection()
                    }
                }
            }
        }
    }

    private fun controlButton() {
        onMainActivity {
            _mainRecyclerView?.controlFloatingButton(
                onButtonClick = {
                    if (inSelection)
                        onClickSelectionAction()
                    else
                        smoothSnapToPosition(0)
                })
        }
    }

    private fun enableRecyclerViewNestedScrolling(shouldEnable: Boolean) {
        _mainRecyclerView?.isNestedScrollingEnabled = shouldEnable
    }

    fun stopScrolling() = _mainRecyclerView?.run {
        suppressLayout(true)
        suppressLayout(false)
    }

    fun uninstallSelectedApp() {
        if (mainViewModel.selectionList.isEmpty()) return
        context?.uninstallPackage(mainViewModel.selectionList.last())
        mainViewModel.setSelectionMode(false)
    }

    fun deleteSelectedBackups() =
        mainViewModel.deleteSelectedBackups(onSuccess = {
            context?.showToast(R.string.successfully_deleted_backups)
        }, onFailure = { message ->
            context?.showToast("${getString(R.string.unsuccessfully_deleted_files)} $message", true)
        })

    fun selectAllItems() {
        adapter.selectAllItems()
        Snackbar.make(
            binding.root, "Selected ${mainViewModel.selectionList.size} items", 1250
        ).show()
    }

    fun getSelectedAppData() = adapter.getCurrentlySelectedItems()
    fun shouldMoveFragmentUp() = _mainRecyclerView?.shouldMoveAtLastCompletelyVisibleItem() ?: false

    fun fixRecyclerViewScrollPosition() {
        if (shouldMoveFragmentUp()) _mainRecyclerView?.slowlyScrollToLastItem()
    }

    override fun onClickSelectionAction() {
        onMainActivity { showConfigureFragment() }
    }

    override fun onCleanUp() {
        binding.saveRecyclerViewState()
        _adapter = null
        _mainRecyclerView?.adapter = null
        _mainRecyclerView = null
    }
}

interface ButtonSelectionAction {
    fun onClickSelectionAction()
}
