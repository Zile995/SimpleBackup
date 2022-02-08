package com.stefan.simplebackup.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.databinding.FragmentAppListBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.viewmodel.AppViewModel
import com.stefan.simplebackup.viewmodel.AppViewModelFactory
import kotlinx.coroutines.*

/**
 * A simple [AppListFragment] class.
 */
class AppListFragment : Fragment(), MenuItemListener {
    // Binding
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    // Coroutine scope
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    // AppData list and ViewModel
    private lateinit var applicationList: MutableList<AppData>

    private lateinit var activity: MainActivity

    private var _appAdapter: AppAdapter? = null
    private val appAdapter get() = _appAdapter!!

    // ViewModel
    private val appViewModel: AppViewModel by activityViewModels {
        val mainApplication = activity.application as DatabaseApplication
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
        scope.launch {
            if (isAdded) {
                setAppViewModelObservers()
                restoreRecyclerViewState()
            }
        }
    }

    private fun bindViews() {
        _appAdapter = AppAdapter(appViewModel)
        //createToolBar()
        createRecyclerView()
        createSwipeContainer()
        createFloatingButton()
    }

    /**
     * - Inicijalizuj gornju traku, ili ToolBar
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun createToolBar() {
        activity.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    val searchView = menuItem?.actionView as SearchView
                    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
                    searchView.queryHint = "Search for apps"

                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            newText?.let { text ->
                                appAdapter.filter(text)
                            }
                            return true
                        }
                    })
                }
                R.id.select_all -> {
                    appViewModel.setSelectedItems(applicationList)
                    appAdapter.notifyDataSetChanged()
                }
            }
            true
        }
    }

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
            setItemViewCacheSize(20)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun setAppViewModelObservers() {
        appViewModel.getAllApps.observe(viewLifecycleOwner) { appList ->
            appList.let {
                applicationList = appList
                appAdapter.setData(appList)
            }
        }
        appViewModel.spinner.observe(viewLifecycleOwner) { value ->
            binding.progressBar.visibility =
                if (value)
                    View.VISIBLE
                else
                    View.GONE
        }
        appViewModel.isSelected.observe(viewLifecycleOwner) { isSelected ->
            activity.toolbar.menu.apply {
                findItem(R.id.select_all).isVisible = isSelected
                findItem(R.id.search).isVisible = !isSelected
            }
        }
    }

    override fun onPause() {
        binding.recyclerView.layoutManager?.onSaveInstanceState()?.let {
            appViewModel.saveRecyclerViewState(it)
        }
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
        _appAdapter = null
    }
}