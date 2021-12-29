package com.stefan.simplebackup.fragments

import android.annotation.SuppressLint
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.activities.MainActivity
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.database.DatabaseApplication
import com.stefan.simplebackup.databinding.FragmentAppListBinding
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

    private var delay: Long = 250

    // ViewModel
    private val appViewModel: AppViewModel by activityViewModels {
        AppViewModelFactory((activity.application as DatabaseApplication).getRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentActivity = this@AppListFragment.requireActivity()
        if (currentActivity is MainActivity) {
            activity = currentActivity
        }
        val recyclerView = binding.recyclerView
        val appAdapter = AppAdapter(requireContext())
        scope.launch {
            if (isAdded) {
                bindViews(recyclerView, appAdapter)
                binding.progressBar.visibility = View.VISIBLE
                if (savedInstanceState != null) {
                    delay = 0
                    binding.progressBar.visibility = View.GONE
                }
                delay(delay)
                setAppViewModelObservers(appAdapter)
            }
        }
    }

    private fun bindViews(recyclerView: RecyclerView, appAdapter: AppAdapter) {
        createToolBar(appAdapter)
        createRecyclerView(recyclerView, appAdapter)
        createSwipeContainer(appAdapter)
        createFloatingButton(recyclerView)
    }

    /**
     * - Inicijalizuj gornju traku, ili ToolBar
     */
    private fun createToolBar(appAdapter: AppAdapter) {
        val toolBar = binding.toolBar

        toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> {
                    val searchView = it?.actionView as SearchView
                    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
                    searchView.queryHint = "Search for apps"

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
    @SuppressLint("NotifyDataSetChanged")
    private fun createRecyclerView(recyclerView: RecyclerView, appAdapter: AppAdapter) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appAdapter
            setItemViewCacheSize(20)
            setHasFixedSize(true)
        }
    }

    private fun createSwipeContainer(appAdapter: AppAdapter) {
        val swipeContainer = binding.swipeRefresh

        swipeContainer.setOnRefreshListener {
            scope.launch {
                launch {
                    activity.refreshPackageList()
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
    private fun createFloatingButton(recyclerView: RecyclerView) {
        val floatingButton = binding.floatingButton
        floatingButton.hide()
        hideButton(floatingButton, recyclerView)

        floatingButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    /**
     * - Sakriva FloatingButton kada se skroluje na gore
     * - Ako je dy > 0, odnosno kada skrolujemo prstom na gore i ako je prikazano dugme, sakrij ga
     * - Ako je dy < 0, odnosno kada skrolujemo prstom na dole i ako je sakriveno dugme, prikaži ga
     */
    private fun hideButton(floatingButton: FloatingActionButton, recyclerView: RecyclerView) {
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

    private fun setAppViewModelObservers(appAdapter: AppAdapter) {
        appViewModel.getAllApps.observe(viewLifecycleOwner, {
            it.let {
                appAdapter.setData(it)
                applicationList = it
            }
        })

        appViewModel.spinner.observe(viewLifecycleOwner, { value ->
            value.let {
                scope.launch {
                    binding.progressBar.visibility =
                        if (value) View.VISIBLE else View.GONE
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }

}