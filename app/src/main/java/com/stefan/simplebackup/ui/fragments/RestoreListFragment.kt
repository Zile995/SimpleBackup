package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.FragmentRestoreListBinding
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.ui.adapters.RestoreAdapter
import com.stefan.simplebackup.ui.adapters.RestoreViewHolder
import com.stefan.simplebackup.utils.main.hideAttachedButton
import com.stefan.simplebackup.utils.main.workerDialog
import com.stefan.simplebackup.viewmodels.RestoreViewModel
import com.stefan.simplebackup.viewmodels.RestoreViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RestoreListFragment : Fragment() {
    // Binding
    private var _binding: FragmentRestoreListBinding? = null
    private val binding get() = _binding!!
    private var _restoreAdapter: RestoreAdapter? = null
    private val restoreAdapter get() = _restoreAdapter!!

    private val restoreViewModel: RestoreViewModel by viewModels {
        RestoreViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("RestoreListFragment", "Creating RestoreListFragment")
        _binding = FragmentRestoreListBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            binding.apply {
                bindViews()
                initObservers()
                restoreRecyclerViewState()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.apply {
            saveRecyclerViewState()
        }
    }

    private fun setRestoreAdapter() {
        val context = requireContext()
        val clickListener = object : OnClickListener {
            override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = restoreAdapter.currentList[position]
                if (restoreAdapter.hasSelectedItems()) {
                    restoreAdapter.doSelection(holder as RestoreViewHolder, item)
                } else {
                    context.workerDialog(
                        title = getString(R.string.confirm_restore),
                        message = getString(R.string.restore_confirmation_message),
                        positiveButtonText = getString(R.string.yes),
                        negativeButtonText = getString(R.string.no)
                    ) {
                        restoreViewModel.startRestoreWorker(item.uid)
                    }
                }
            }

            override fun onLongItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = restoreAdapter.currentList[position]
                restoreViewModel.setSelectionMode(true)
                restoreAdapter.doSelection(holder as RestoreViewHolder, item)
            }
        }
        _restoreAdapter = RestoreAdapter(
            restoreViewModel.selectionList,
            clickListener,
            restoreViewModel.setSelectionMode
        )
    }

    private fun FragmentRestoreListBinding.bindViews() {
        bindRecyclerView()
        bindSwipeContainer()
        bindFloatingButton()
    }

    private fun FragmentRestoreListBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val refresh = launch {
                }
                refresh.join()
                swipeRefresh.isRefreshing = false
                delay(250)
            }
        }
    }

    private fun FragmentRestoreListBinding.bindRecyclerView() {
        setRestoreAdapter()
        restoreRecyclerView.apply {
            adapter = restoreAdapter
            setHasFixedSize(true)
        }
    }

    private fun FragmentRestoreListBinding.bindFloatingButton() {
        floatingButton.hide()
        restoreRecyclerView.hideAttachedButton(floatingButton)

        floatingButton.setOnClickListener {
            restoreRecyclerView.smoothScrollToPosition(0)
        }
    }

    private fun FragmentRestoreListBinding.saveRecyclerViewState() {
        restoreRecyclerView.layoutManager?.onSaveInstanceState()?.let {
            restoreViewModel.saveRecyclerViewState(it)
        }
    }

    private fun FragmentRestoreListBinding.restoreRecyclerViewState() {
        if (restoreViewModel.isStateInitialized) {
            restoreRecyclerView.layoutManager?.onRestoreInstanceState(restoreViewModel.restoreRecyclerViewState)
        }
    }

    private fun FragmentRestoreListBinding.initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    restoreViewModel.startPackagePolling()
                }
            }
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    restoreViewModel.isSelected.collect { value ->
                        batchRestore.visibility = if (value) View.VISIBLE else View.GONE
                    }
                }
                restoreViewModel.spinner.collect { value ->
                    if (value) {
                        progressBar.visibility = View.VISIBLE
                    } else {
                        progressBar.visibility = View.GONE
                        restoreViewModel.localApps.collect { appList ->
                            restoreAdapter.submitList(appList)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("RestoreListFragment", "Destroying RestoreListFragment")
        _binding = null
        _restoreAdapter = null
    }
}