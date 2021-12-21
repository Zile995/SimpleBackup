package com.stefan.simplebackup.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.adapter.RestoreAdapter
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.databinding.FragmentRestoreListBinding
import com.stefan.simplebackup.utils.FileUtil
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class RestoreListFragment : Fragment() {

    companion object {
        private const val ROOT: String = "SimpleBackup/local"
    }

    // Binding
    private var _binding: FragmentRestoreListBinding? = null
    private val binding get() = _binding!!

    // Coroutine scope
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private var applicationList = mutableListOf<Application>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var toolBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var restoreAdapter: RestoreAdapter
    private lateinit var floatingButton: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        // Inflate the layout for this fragment
        _binding = FragmentRestoreListBinding.inflate(inflater, container, false)
        createToolBar(binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scope.launch {
            delay(200)
            if (isAdded) {
                bindViews(binding)
                launch {
                    getStoredPackages()
                }.join()
                launch {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    updateAdapter()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }

    private fun bindViews(binding: FragmentRestoreListBinding) {
        with(binding) {
            createProgressBar(this)
            createRecyclerView(this)
            createSwipeContainer(this)
            createFloatingButton(this)
        }

    }

    private fun createSwipeContainer(binding: FragmentRestoreListBinding) {
        swipeContainer = binding.swipeRefresh

        swipeContainer.setOnRefreshListener {
            scope.launch {
                val refresh = launch {
                    refreshStoredPackages()
                }
                refresh.join()
                launch {
                    swipeContainer.isRefreshing = false
                    delay(250)
                    updateAdapter()
                }
            }
        }
    }

    private fun createRecyclerView(binding: FragmentRestoreListBinding) {
        recyclerView = binding.restoreRecyclerView
        restoreAdapter = RestoreAdapter(requireContext())
        recyclerView.adapter = restoreAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun updateAdapter() {
        restoreAdapter.updateList(applicationList)
    }

    private suspend fun refreshStoredPackages() {
        withContext(Dispatchers.IO) {
            getStoredPackages()
        }
    }

    private fun getStoredPackages() {
        val tempApps = mutableListOf<Application>()
        val path = (requireContext().getExternalFilesDir(null)!!.absolutePath).run {
            substring(0, indexOf("Android")).plus(ROOT)
        }
        val dir = File(path)
        if (dir.exists()) {
            val listFiles = dir.listFiles()

            if (!listFiles.isNullOrEmpty()) {
                listFiles.forEach { file ->
                    if (file.isDirectory) {
                        Log.d("listdirs", file.toString())
                        file.listFiles()?.filter {
                            it.isFile
                        }?.forEach {
                            var application: Application
                            if (it.absolutePath.contains(".json")) {
                                Log.d("listfiles", it.toString())
                                File(it.absolutePath).inputStream().bufferedReader()
                                    .use { reader ->
                                        val string = reader.readLine()
                                        Log.d("asdf", string)
                                        application = Json.decodeFromString(string)
                                        tempApps.add(application)
                                    }
                            }
                        }
                    }
                }
            }
            applicationList = tempApps
            applicationList.sortBy { it.getName() }
        } else {
            with(FileUtil) {
                createDirectory(path)
                createFile(path.plus("/.nomedia"))
            }
        }
    }

    private fun createProgressBar(binding: FragmentRestoreListBinding) {
        progressBar = binding.progressBar
    }

    private fun createToolBar(binding: FragmentRestoreListBinding) {
        toolBar = binding.toolBar
        toolBar.setTitleTextAppearance(requireContext(), R.style.ActionBarTextAppearance)

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
                            return true
                        }
                    })
                }
            }
            true
        }
    }

    private fun createFloatingButton(binding: FragmentRestoreListBinding) {
        floatingButton = binding.floatingButton
        floatingButton.hide()

        hideButton(recyclerView)

        floatingButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

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
}