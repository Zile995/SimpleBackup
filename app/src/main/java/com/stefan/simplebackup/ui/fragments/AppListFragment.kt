package com.stefan.simplebackup.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.databinding.FragmentAppListBinding
import com.stefan.simplebackup.ui.activities.BackupActivity
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.adapters.AppAdapter
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.ui.notifications.BackupNotificationBuilder
import com.stefan.simplebackup.utils.backup.BACKUP_ARGUMENT
import com.stefan.simplebackup.utils.backup.BACKUP_REQUEST_TAG
import com.stefan.simplebackup.utils.main.BitmapUtil
import com.stefan.simplebackup.viewmodels.AppViewModel
import com.stefan.simplebackup.viewmodels.AppViewModelFactory
import kotlinx.coroutines.*

/**
 * A simple [AppListFragment] class.
 */
class AppListFragment : Fragment(), MenuItemListener {
    // Binding
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    // Coroutine scope
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // AppData list and ViewModel
    private lateinit var applicationList: MutableList<AppData>

    private lateinit var activity: MainActivity

    private var _appAdapter: AppAdapter? = null
    private val appAdapter get() = _appAdapter!!

    private var isSearching = false

    // ViewModel
    private val appViewModel: AppViewModel by activityViewModels {
        val mainApplication = activity.application as MainApplication
        AppViewModelFactory(mainApplication)
    }

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
        // Inflate the layout for this fragment
        println("Creating AppListFragment")
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        bindViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner
            .lifecycleScope.launch {
            setAppViewModelObservers()
            restoreRecyclerViewState()
            if (savedInstanceState != null) {
                isSearching = savedInstanceState.getBoolean("isSearching")
            }
            if (isSearching) {
                binding.searchInput.requestFocus()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.recyclerView.layoutManager?.onSaveInstanceState()?.let {
            appViewModel.saveRecyclerViewState(it)
        }
        if (binding.searchInput.hasFocus()) {
            isSearching = true
        }
        outState.putBoolean("isSearching", isSearching)
    }

    private fun bindViews() {
        setAppAdapter()
        //createToolBar()
        createRecyclerView()
        createSwipeContainer()
        createFloatingButton()
        setBackupChip()
    }

    private fun setBackupChip() {
        binding.batchBackup.setOnClickListener {
            appViewModel.createLocalBackup()
        }
    }

    private val workInfoObserver: Observer<List<WorkInfo>>
        get() {
            return Observer { workInfoList ->
                if (workInfoList.isEmpty())
                    return@Observer
                val workInfo = workInfoList[0]
                if (workInfo.state.isFinished
                    && workInfo.outputData.getBoolean(
                        BACKUP_ARGUMENT,
                        false
                    )
                ) {
                    Log.d(
                        "Observer",
                        "Got this data: ${
                            workInfo.outputData.getBoolean(
                                BACKUP_ARGUMENT,
                                false
                            )
                        }"
                    )
                    val backupNotificationBuilder =
                        BackupNotificationBuilder(requireContext(), false)
                    backupNotificationBuilder.showBackupFinishedNotification()
                    appViewModel.getWorkManager.pruneWork()
                }
            }
        }

    private fun setAppAdapter() {
        _appAdapter = AppAdapter(appViewModel, object : OnClickListener {
            override fun onItemViewClick(holder: AppAdapter.AppViewHolder, position: Int) {
                val item = applicationList[position]
                if (appViewModel.hasSelectedItems()) {
                    appViewModel.doSelection(holder, item)
                } else {
                    viewLifecycleOwner
                        .lifecycleScope.launch {
                        val context = holder.getContext
                        BitmapUtil.saveBigBitmap(item, context)
                        val intent = Intent(context, BackupActivity::class.java)
                        intent.putExtra("application", item)
                        context.startActivity(intent)
                    }
                }
            }

            override fun onLongItemViewClick(holder: AppAdapter.AppViewHolder, position: Int) {
                val item = applicationList[position]
                appViewModel.setSelectionMode(true)
                appViewModel.doSelection(holder, item)
            }
        })
    }

    /**
     * - Inicijalizuj gornju traku, ili ToolBar
     */
    @SuppressLint("NotifyDataSetChanged")
//    private fun createToolBar() {
//        binding.toolBar.setOnMenuItemClickListener { menuItem ->
//            Log.d("Search", "toolbar item clicked")
//            when (menuItem.itemId) {
//                R.id.search -> {
//                    val searchView = menuItem?.actionView as SearchView
//                    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
//                    searchView.queryHint = "Search for apps"
//
//                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//                        override fun onQueryTextSubmit(query: String?): Boolean {
//                            return false
//                        }
//
//                        override fun onQueryTextChange(newText: String?): Boolean {
//                            newText?.let { text ->
//                                appAdapter.filter(text)
//                            }
//                            return true
//                        }
//                    })
//                }
//                R.id.select_all -> {
//                    appViewModel.setSelectedItems(applicationList)
//                    appAdapter.notifyDataSetChanged()
//                }
//            }
//            true
//        }
//    }

    override fun searchQuery(text: String) {
        println("processing search $text")
        appAdapter.filter(text)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun selectAll() {
        appViewModel.setSelectedItems(applicationList)
        appAdapter.notifyDataSetChanged()
    }

    /**
     * - Inicijalizuj recycler view
     */
    private fun createRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appAdapter
            setItemViewCacheSize(10)
            setHasFixedSize(true)
        }
    }

    private fun createSwipeContainer() {
        val swipeContainer = binding.swipeRefresh

        swipeContainer.setOnRefreshListener {
            scope.launch {
                launch {
                    appViewModel.refreshPackageList()
                }.join()
                launch {
                    swipeContainer.isRefreshing = false
                    delay(200)
                    appAdapter.submitList(applicationList)
                }
            }
        }
    }

    /**
     * - Inicijalizuj Floating dugme
     */
    private fun createFloatingButton() {
        val floatingButton = binding.floatingButton
        floatingButton.hide()
        hideButton()

        floatingButton.setOnClickListener {
            binding.recyclerView.smoothScrollToPosition(0)
        }
    }

    /**
     * - Sakriva FloatingButton kada se skroluje na gore
     * - Ako je dy > 0, odnosno kada skrolujemo prstom na gore i ako je prikazano dugme, sakrij ga
     * - Ako je dy < 0, odnosno kada skrolujemo prstom na dole i ako je sakriveno dugme, prikaži ga
     */
    private fun hideButton() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0 && binding.floatingButton.isShown) {
                    binding.floatingButton.hide()

                } else if (dy < 0 && !binding.floatingButton.isShown) {
                    binding.floatingButton.show()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // Ako ne može da skroluje više na dole (1 je down direction) i ako može ma gore (-1 up direction)
                if (!recyclerView.canScrollVertically(1)
                    && recyclerView.canScrollVertically(-1)
                    && newState == RecyclerView.SCROLL_STATE_IDLE
                ) {
                    binding.floatingButton.show()
                } else if (recyclerView.canScrollVertically(1)
                    && !recyclerView.canScrollVertically(
                        -1
                    )
                ) {
                    binding.floatingButton.hide()
                }
            }
        })
    }

    private fun restoreRecyclerViewState() {
        if (appViewModel.isStateInitialized) {
            binding.recyclerView.layoutManager?.onRestoreInstanceState(appViewModel.restoreRecyclerViewState)
        }
    }

    private fun setAppViewModelObservers() {
        appViewModel.getWorkManager.getWorkInfosByTagLiveData(BACKUP_REQUEST_TAG)
            .observe(viewLifecycleOwner, workInfoObserver)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appViewModel.spinner.collect { value ->
                    binding.progressBar.visibility =
                        if (value)
                            View.VISIBLE
                        else {
                            launch {
                                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                                    appViewModel.getAllApps.collect { appList ->
                                        applicationList = appList
                                        appAdapter.setData(appList)
                                    }
                                }
                            }
                            View.GONE
                        }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appViewModel.isSelected.collect { isSelected ->
                    binding.batchBackup.visibility =
                        if (isSelected)
                            View.VISIBLE
                        else
                            View.GONE
//            binding.toolBar.menu.apply {
//                findItem(R.id.select_all).isVisible = isSelected
//                findItem(R.id.search).isVisible = !isSelected
//            }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
        _appAdapter = null
    }
}