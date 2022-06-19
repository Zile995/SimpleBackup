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
import com.stefan.simplebackup.databinding.FragmentLocalBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.adapters.BaseAdapter
import com.stefan.simplebackup.ui.adapters.BaseViewHolder
import com.stefan.simplebackup.ui.adapters.HolderType
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.ui.viewmodels.LocalViewModelFactory
import com.stefan.simplebackup.utils.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocalFragment : Fragment(), RecyclerViewSaver<FragmentLocalBinding> {
    // Binding
    private var _binding: FragmentLocalBinding? = null
    private val binding get() = _binding!!
    private var _localAdapter: BaseAdapter? = null
    private val localAdapter get() = _localAdapter!!

    private val localViewModel: LocalViewModel by viewModels {
        LocalViewModelFactory(requireActivity().application as MainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LocalFragment", "Creating LocalFragment")
        _binding = FragmentLocalBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            binding.apply {
                bindViews()
                setActivityCallBacks()
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

    private fun RecyclerView.setBaseAdapter() {
        _localAdapter = BaseAdapter(
            HolderType.LOCAL,
            localViewModel.selectionList,
            localViewModel.setSelectionMode
        ).apply {
            val context = requireContext()
            clickListener = object : OnClickListener {
                override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = localAdapter.currentList[position]
                    if (localAdapter.hasSelectedItems()) {
                        localAdapter.doSelection(holder as BaseViewHolder, item)
                    } else {
                        context.workerDialog(
                            title = getString(R.string.confirm_restore),
                            message = getString(R.string.restore_confirmation_message),
                            positiveButtonText = getString(R.string.yes),
                            negativeButtonText = getString(R.string.no)
                        ) {
                            localViewModel.startRestoreWorker(item.uid)
                        }
                    }
                }

                override fun onLongItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = localAdapter.currentList[position]
                    localViewModel.setSelectionMode(true)
                    localAdapter.doSelection(holder as BaseViewHolder, item)
                }
            }
        }
        adapter = localAdapter
    }

    private fun FragmentLocalBinding.bindViews() {
        bindRecyclerView()
        bindSwipeContainer()
    }

    private fun FragmentLocalBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val refresh = launch {
                    // TODO: Could be deleted
                }
                refresh.join()
                swipeRefresh.isRefreshing = false
                delay(250)
            }
        }
    }

    private fun FragmentLocalBinding.bindRecyclerView() {
        localRecyclerView.apply {
            setBaseAdapter()
            setHasFixedSize(true)
        }
    }

    private fun FragmentLocalBinding.setActivityCallBacks() {
        onActivityCallback<MainActivity> {
            localRecyclerView.controlFloatingButton()
        }
    }

    override fun FragmentLocalBinding.saveRecyclerViewState() {
        localRecyclerView.onSaveRecyclerViewState { stateParcelable ->
            localViewModel.saveRecyclerViewState(stateParcelable)
        }
    }

    override fun FragmentLocalBinding.restoreRecyclerViewState() {
        localRecyclerView.onRestoreRecyclerViewState(localViewModel.savedRecyclerViewState)
    }

    private fun FragmentLocalBinding.initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    localViewModel.startPackagePolling()
                }
            }
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    localViewModel.isSelected.collect { isSelected ->
                        batchRestore.isVisible = isSelected
                    }
                }
                localViewModel.spinner.collect { isSpinning ->
                    //delay(350)
                    progressBar.isVisible = isSpinning
                    if (!isSpinning) {
                        localViewModel.localApps.collect { appList ->
                            localAdapter.submitList(appList)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        Log.d("LocalFragment", "Destroying LocalFragment")
        super.onDestroyView()
        _localAdapter = null
        _binding = null
    }
}