package com.stefan.simplebackup.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.adapter.RestoreAdapter
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.databinding.FragmentRestoreListBinding
import com.stefan.simplebackup.utils.FileUtil.createDirectory
import com.stefan.simplebackup.utils.FileUtil.createFile
import com.stefan.simplebackup.utils.FileUtil.jsonToApp
import com.stefan.simplebackup.utils.backup.ROOT
import kotlinx.coroutines.*
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class RestoreListFragment : Fragment() {
    // Binding
    private var _binding: FragmentRestoreListBinding? = null
    private val binding get() = _binding!!

    // Restore List Adapter
    private var _restoreAdapter: RestoreAdapter? = null
    private val restoreAdapter get() = _restoreAdapter!!

    // Coroutine scope
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private var applicationList = mutableListOf<AppData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        _binding = FragmentRestoreListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _restoreAdapter = RestoreAdapter(requireContext())
        scope.launch {
            if (isAdded) {
                bindViews()
                launch {
                    getStoredPackages()
                }.join()
                launch {
                    binding.progressBar.visibility = ProgressBar.INVISIBLE
                    restoreAdapter.setData(applicationList)
                }
            }
        }
    }

    private fun bindViews() {
        createToolBar()
        createRecyclerView()
        createSwipeContainer()
        createFloatingButton()
    }

    private fun createSwipeContainer() {
        binding.swipeRefresh.setOnRefreshListener {
            scope.launch {
                val refresh = launch {
                    getStoredPackages()
                }
                refresh.join()
                launch {
                    binding.swipeRefresh.isRefreshing = false
                    delay(250)
                    restoreAdapter.setData(applicationList)
                }
            }
        }
    }

    private fun createRecyclerView() {
        binding.restoreRecyclerView.apply {
            adapter = restoreAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private suspend fun getStoredPackages() {
        withContext(Dispatchers.IO) {
            val tempApps = mutableListOf<AppData>()
            requireContext().getExternalFilesDir(null)?.absolutePath?.run {
                substring(0, indexOf("Android")) + ROOT
            }?.let { path ->
                val dir = File(path)
                if (dir.exists()) {
                    dir.listFiles()?.forEach { appDirList ->
                        appDirList.listFiles()?.filter { appDirFile ->
                            appDirFile.isFile && appDirFile.extension == "json"
                        }?.map { jsonFile ->
                            jsonToApp(jsonFile).collect { app ->
                                tempApps.add(app)
                            }
                        }
                    }
                    applicationList.clear()
                    applicationList.addAll(tempApps)
                    applicationList.sortBy { it.getName() }
                } else {
                    createDirectory(path)
                    createFile("$path/.nomedia")
                }
            }
        }
    }

    private fun createToolBar() {
        binding.toolBar.setOnMenuItemClickListener { menuItem ->
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
                            restoreAdapter.filter.filter(newText)
                            return true
                        }
                    })
                }
            }
            true
        }
    }

    private fun createFloatingButton() {
        binding.floatingButton.hide()
        hideButton()

        binding.floatingButton.setOnClickListener {
            binding.restoreRecyclerView.smoothScrollToPosition(0)
        }
    }

    private fun hideButton() {
        binding.restoreRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
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
                if (!recyclerView.canScrollVertically(1) && recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.floatingButton.show()
                } else if (recyclerView.canScrollVertically(1) && !recyclerView.canScrollVertically(
                        -1
                    )
                ) {
                    binding.floatingButton.hide()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
        _restoreAdapter = null
    }
}