package com.stefan.simplebackup.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.activities.MainActivity
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.databinding.FragmentAppListBinding
import com.stefan.simplebackup.utils.SearchUtil
import com.stefan.simplebackup.viewmodel.AppViewModel
import com.stefan.simplebackup.viewmodel.AppViewModelFactory
import kotlinx.coroutines.*

/**
 * A simple [AppListFragment] class.
 */
class AppListFragment : Fragment(), DefaultLifecycleObserver {
    // Binding
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity

    // Coroutine scope
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    // Application data list and ViewModel
    private var applicationList = mutableListOf<Application>()

    // ViewModel
    private val appViewModel: AppViewModel by viewModels {
        AppViewModelFactory((activity.application as DatabaseApplication).getRepository)
    }

    // UI
    private lateinit var toolBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var floatingButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    // RecyclerView
    private lateinit var appAdapter: AppAdapter
    private lateinit var recyclerView: RecyclerView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        createToolBar(binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity = this@AppListFragment.requireActivity() as MainActivity
        scope.launch {
            if (isAdded) {
                bindViews(binding)
                progressBar.visibility = View.VISIBLE
                if (savedInstanceState != null) {
                    progressBar.visibility = View.GONE
                }
                delay(250)
                setAppViewModelObservers()
            }
        }
    }

    private fun bindViews(binding: FragmentAppListBinding) {
        with(binding) {
            createProgressBar(this)
            createRecyclerView(this)
            createSwipeContainer(this)
            createFloatingButton(this)
        }
    }

    /**
     * - Inicijalizuj gornju traku, ili ToolBar
     */
    private fun createToolBar(binding: FragmentAppListBinding) {
        toolBar = binding.toolBar
        toolBar.setTitleTextAppearance(requireContext(), R.style.ActionBarTextAppearance)

        toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> {
                    val searchView = it?.actionView as SearchView
                    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
                    searchView.queryHint = "Search for apps"
                    searchView.setBackgroundColor(Color.TRANSPARENT)

                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            if (applicationList.size > 0) {
                                SearchUtil.search(applicationList, requireContext(), newText)
                            }
                            return true
                        }
                    })
                }
            }
            true
        }
    }

    private fun createProgressBar(binding: FragmentAppListBinding) {
        progressBar = binding.progressBar
    }

    /**
     * - Inicijalizuj recycler view
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun createRecyclerView(
        binding: FragmentAppListBinding
    ) {
        recyclerView = binding.recyclerView
        appAdapter = AppAdapter(requireContext())
        recyclerView.adapter = appAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun createSwipeContainer(binding: FragmentAppListBinding) {
        swipeContainer = binding.swipeRefresh

        swipeContainer.setOnRefreshListener {
            scope.launch {
                launch {
                    refreshPackageList()
                }.join()
                launch {
                    swipeContainer.isRefreshing = false
                    delay(200)
                    updateAdapter()
                }
            }
        }
    }

    /**
     * - Inicijalizuj Floating dugme
     */
    private fun createFloatingButton(binding: FragmentAppListBinding) {
        floatingButton = binding.floatingButton
        floatingButton.hide()
        hideButton(recyclerView)

        floatingButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    /**
     * - Sakriva FloatingButton kada se skroluje na gore
     * - Ako je dy > 0, odnosno kada skrolujemo prstom na gore i ako je prikazano dugme, sakrij ga
     * - Ako je dy < 0, odnosno kada skrolujemo prstom na dole i ako je sakriveno dugme, prikaži ga
     */
    private fun hideButton(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0 && floatingButton.isShown) {
                    floatingButton.hide()


                } else if (dy < 0 && !floatingButton.isShown) {
                    floatingButton.show()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // Ako ne može da skroluje više na dole (1 je down direction) i ako može ma gore (-1 up direction)
                if (!recyclerView.canScrollVertically(1) && recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    floatingButton.show()
                } else if (recyclerView.canScrollVertically(1) && !recyclerView.canScrollVertically(
                        -1
                    )
                ) {
                    floatingButton.hide()
                }
            }
        })
    }

    private fun setAppViewModelObservers() {
        appViewModel.getAllApps.observe(viewLifecycleOwner, {
            it?.let {
                applicationList = it
                updateAdapter()
            }
        })

        appViewModel.spinner.observe(viewLifecycleOwner, { value ->
            value.let {
                scope.launch {
                    progressBar.visibility =
                        if (value) View.VISIBLE else View.GONE
                }
            }
        })
    }

    /**
     *  - Prosleđuje AppAdapter adapteru novu listu i obaveštava RecyclerView da je lista promenjena
     */
    private suspend fun refreshPackageList() {
        withContext(Dispatchers.IO) {
            launch {
            }
        }
    }

    private fun updateAdapter() {
        appAdapter.updateList(applicationList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }

}