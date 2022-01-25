package com.stefan.simplebackup.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
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
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.databinding.FragmentAppListBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.viewmodel.AppViewModel
import com.stefan.simplebackup.viewmodel.AppViewModelFactory
import kotlinx.coroutines.*

/**
 * A simple [AppListFragment] class.
 */
class AppListFragment : Fragment() {
    // Binding
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity

    // Coroutine scope
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    // Application data list and ViewModel
    private var applicationList = mutableListOf<Application>()

    private var _appAdapter: AppAdapter? = null
    private val appAdapter get() = _appAdapter!!

    // ViewModel
    private val appViewModel: AppViewModel by activityViewModels {
        val mainApplication = activity.application as DatabaseApplication
        AppViewModelFactory(mainApplication)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        println("Creating AppListFragment")
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val currentActivity = requireActivity()
        if (currentActivity is MainActivity) {
            activity = currentActivity
        }
        appViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _appAdapter = AppAdapter(appViewModel)
        scope.launch {
            if (isAdded) {
                if (savedInstanceState != null) {
                    binding.progressBar.visibility = View.GONE
                }
                bindViews()
                setAppViewModelObservers()
                restoreRecyclerViewState()
            }
        }
    }

    private fun bindViews() {
        createToolBar()
        createRecyclerView()
        createSwipeContainer()
        createFloatingButton()
    }

    /**
     * - Inicijalizuj gornju traku, ili ToolBar
     */
    private fun createToolBar() {
        val toolBar = binding.toolBar

        toolBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    val selectAllVisibility = toolBar.menu.findItem(R.id.select_all).isVisible
                    val searchView = menuItem?.actionView as SearchView
                    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
                    searchView.queryHint = "Search for apps"

                    menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                        override fun onMenuItemActionExpand(menuItem: MenuItem?): Boolean {
                            toolBar.menu.findItem(R.id.select_all).isVisible = false
                            return true
                        }

                        override fun onMenuItemActionCollapse(menuItem: MenuItem?): Boolean {
                            toolBar.menu.findItem(R.id.select_all).isVisible =
                                selectAllVisibility
                            return true
                        }
                    })

                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            appAdapter.filter.filter(newText)
                            return true
                        }
                    })
                }
            }
            true
        }
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

    private fun setAppViewModelObservers() {
        appViewModel.getAllApps.observe(viewLifecycleOwner, { appList ->
            appList.let {
                appAdapter.setData(appList)
                applicationList = appList
            }
        })
        appViewModel.spinner.observe(viewLifecycleOwner, { value ->
            scope.launch {
                binding.progressBar.visibility =
                    if (value) View.VISIBLE else View.GONE
            }
        })
        appViewModel.isSelected.observe(viewLifecycleOwner, { isSelected ->
            binding.toolBar.menu.findItem(R.id.select_all).apply {
                isVisible = isSelected
            }
        })
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