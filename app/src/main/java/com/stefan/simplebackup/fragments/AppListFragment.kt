package com.stefan.simplebackup.fragments

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.activities.MainActivity
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.AppInfo
import com.stefan.simplebackup.data.AppViewModel
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.databinding.FragmentAppListBinding
import com.stefan.simplebackup.utils.SearchUtil
import kotlinx.coroutines.*

/**
 * A simple [AppListFragment] class.
 */
class AppListFragment : Fragment() {
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    private var appInfo = AppInfo

    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private var applicationList = mutableListOf<Application>()
    private lateinit var appViewModel: AppViewModel

    // UI
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var appAdapter: AppAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingButton: FloatingActionButton
    private lateinit var progressBar: ProgressBar

    // Flags
    private val flags: Int = PackageManager.GET_META_DATA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(binding)
        scope.launch {
            val get = launch {
                if (this@AppListFragment.isAdded) {
                    makeDatabase()
                    setAppViewModel()
                }
            }
            get.join()
            launch {
                progressBar.visibility = View.GONE
                updateAdapter()
            }
        }
    }

    /**
     * - Kreiraj menu i podesi listener za search polje
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_app_bar, menu)
        val menuItem = menu.findItem(R.id.search)
        val searchView = menuItem?.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.queryHint = "Search for apps"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (applicationList.size > 0) {
                    SearchUtil.search(
                        applicationList,
                        this@AppListFragment.requireContext(),
                        newText
                    )
                }
                return true
            }
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun bindViews(binding: FragmentAppListBinding) {
        with(binding) {
            createProgressBar(this)
            createRecyclerView(this)
            createSwipeContainer(this)
            createFloatingButton(this)
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
        appAdapter = AppAdapter(this.requireActivity())
        recyclerView.adapter = appAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)
        recyclerView.layoutManager = LinearLayoutManager(context)
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

    private suspend fun makeDatabase() {
        if (!appInfo.databaseExists(requireContext()))
            appInfo.makeDatabase(MainActivity.result.await())
    }

    private suspend fun setAppViewModel() {
        appViewModel =
            ViewModelProvider(this).get(AppViewModel::class.java)
        applicationList = appViewModel.getAppList()
    }

    /**
     *  - Prosleđuje AppAdapter adapteru novu listu i obaveštava RecyclerView da je lista promenjena
     */
    private suspend fun refreshPackageList() {
        withContext(Dispatchers.IO) {
            launch {
                AppInfo.getInstalledApplications(flags)
                    .setPackageList(this@AppListFragment.requireContext())
            }
        }
    }

    private fun updateAdapter() {
        appAdapter.updateList(applicationList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}