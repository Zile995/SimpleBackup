package com.stefan.simplebackup.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentAppListBinding
import com.stefan.simplebackup.ui.activities.AppDetailActivity
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.activities.ProgressActivity
import com.stefan.simplebackup.ui.adapters.AppAdapter
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.utils.main.BitmapUtil
import com.stefan.simplebackup.viewmodels.AppViewModel
import com.stefan.simplebackup.viewmodels.AppViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A simple [AppListFragment] class.
 */
class AppListFragment : Fragment() {
    // Binding
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity

    private var _appAdapter: AppAdapter? = null
    private val appAdapter get() = _appAdapter!!

    // ViewModel
    private val appViewModel: AppViewModel by activityViewModels {
        AppViewModelFactory(activity.application as MainApplication)
    }

    private var isSearching = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val currentActivity = requireActivity()
        if (currentActivity is MainActivity) {
            activity = currentActivity
        }
        appViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        println("Creating AppListFragment")

        _binding = FragmentAppListBinding
            .inflate(inflater, container, false)
        setAppAdapter()
        binding.bindViews()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner
            .lifecycleScope.launch {
                binding.apply {
                    setViewModelObservers()
                    restoreRecyclerViewState()
                    if (savedInstanceState != null) {
                        isSearching = savedInstanceState.getBoolean("isSearching")
                    }
                    if (isSearching) {
                        searchInput.requestFocus()
                    }
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.apply {
            recyclerView.layoutManager?.onSaveInstanceState()?.let {
                appViewModel.saveRecyclerViewState(it)
            }
            if (searchInput.hasFocus()) {
                isSearching = true
            }
        }
        outState.putBoolean("isSearching", isSearching)
    }

    private fun FragmentAppListBinding.bindViews() {
        bindRecyclerView()
        bindSwipeContainer()
        bindFloatingButton()
        bindBackupChip()
    }

    private fun FragmentAppListBinding.bindBackupChip() {
        batchBackup.setOnClickListener {
            requireContext().apply {
                startActivity(Intent(this, ProgressActivity::class.java).apply {
                    putExtra("selection_list", appViewModel.selectionList.toTypedArray())
                })
            }
        }
    }

    private fun setAppAdapter() {
        _appAdapter = AppAdapter(appViewModel, object : OnClickListener {
            override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = appAdapter.currentList[position]
                if (appViewModel.hasSelectedItems()) {
                    appViewModel.doSelection(holder, item)
                } else {
                    viewLifecycleOwner
                        .lifecycleScope.launch {
                            val context = (holder as AppAdapter.AppViewHolder).getContext
                            BitmapUtil.saveIfBigBitmap(item, context)
                            val intent = Intent(context, AppDetailActivity::class.java)
                            intent.putExtra("application", item)
                            context.startActivity(intent)
                        }
                }
            }

            override fun onLongItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = appAdapter.currentList[position]
                appViewModel.setSelectionMode(true)
                appViewModel.doSelection(holder, item)
            }
        })
    }

    /**
     * - Inicijalizuj recycler view
     */
    private fun FragmentAppListBinding.bindRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appAdapter
            setItemViewCacheSize(5)
            setHasFixedSize(true)
        }
    }

    private fun FragmentAppListBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                launch {
                    appViewModel.refreshPackageList()
                }.join()
                launch {
                    swipeRefresh.isRefreshing = false
                    delay(200)
                }
            }
        }
    }

    /**
     * - Inicijalizuj Floating dugme
     */
    private fun FragmentAppListBinding.bindFloatingButton() {
        floatingButton.hide()
        hideButton()

        floatingButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    /**
     * - Sakriva FloatingButton kada se skroluje na gore
     * - Ako je dy > 0, odnosno kada skrolujemo prstom na gore i ako je prikazano dugme, sakrij ga
     * - Ako je dy < 0, odnosno kada skrolujemo prstom na dole i ako je sakriveno dugme, prikaži ga
     */
    private fun FragmentAppListBinding.hideButton() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0 && binding.floatingButton.isShown) {
                    floatingButton.hide()

                } else if (dy < 0 && !binding.floatingButton.isShown) {
                    floatingButton.show()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // Ako ne može da skroluje više na dole (1 je down direction) i ako može ma gore (-1 up direction)
                if (!recyclerView.canScrollVertically(1)
                    && recyclerView.canScrollVertically(-1)
                    && newState == RecyclerView.SCROLL_STATE_IDLE
                ) {
                    floatingButton.show()
                } else if (recyclerView.canScrollVertically(1)
                    && !recyclerView.canScrollVertically(
                        -1
                    )
                ) {
                    floatingButton.hide()
                }
            }
        })
    }

    private fun FragmentAppListBinding.restoreRecyclerViewState() {
        if (appViewModel.isStateInitialized) {
            recyclerView.layoutManager?.onRestoreInstanceState(appViewModel.restoreRecyclerViewState)
        }
    }

    private fun FragmentAppListBinding.setViewModelObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    appViewModel.isSelected.collect { isSelected ->
                        batchBackup.visibility =
                            if (isSelected)
                                View.VISIBLE
                            else
                                View.GONE
                    }
                }
                appViewModel.spinner.collect { value ->
                    if (value)
                        progressBar.visibility = View.VISIBLE
                    else {
                        progressBar.visibility = View.GONE
                        appViewModel.getAllApps.collect { appList ->
                            appAdapter.submitList(appList)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _appAdapter = null
    }
}