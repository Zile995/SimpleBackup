package com.stefan.simplebackup.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.FragmentRestoreListBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.adapters.AppAdapter
import com.stefan.simplebackup.ui.adapters.OnClickListener
import com.stefan.simplebackup.ui.adapters.RestoreAdapter
import com.stefan.simplebackup.utils.backup.ROOT
import com.stefan.simplebackup.utils.main.FileUtil
import com.stefan.simplebackup.utils.main.JsonUtil
import com.stefan.simplebackup.utils.main.showRestoreDialog
import com.stefan.simplebackup.viewmodels.RestoreViewModel
import com.stefan.simplebackup.viewmodels.RestoreViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class RestoreListFragment : Fragment() {
    // Binding
    private var _binding: FragmentRestoreListBinding? = null
    private val binding get() = _binding!!

    private lateinit var activity: MainActivity

    // Restore List Adapter
    private var _restoreAdapter: RestoreAdapter? = null
    private val restoreAdapter get() = _restoreAdapter!!

    private val restoreViewModel: RestoreViewModel by viewModels {
        RestoreViewModelFactory(activity.application as MainApplication)
    }

    private var applicationList = mutableListOf<AppData>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val currentActivity = requireActivity()
        if (currentActivity is MainActivity) {
            activity = currentActivity
        }
        restoreViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentRestoreListBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _restoreAdapter = RestoreAdapter(object : OnClickListener {
            override fun onItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = restoreAdapter.currentList[position]
                if (restoreViewModel.hasSelectedItems()) {
                    restoreViewModel.doSelection(holder, item)
                } else {
                    restoreViewModel.addSelectedItem(item.packageName)
                    requireContext().showRestoreDialog {
                        restoreViewModel.startRestoreWorker()
                    }
                    restoreViewModel.selectionList.clear()
                }
            }

            override fun onLongItemViewClick(holder: RecyclerView.ViewHolder, position: Int) {
                val item = restoreAdapter.currentList[position]
                restoreViewModel.setSelectionMode(true)
                restoreViewModel.doSelection(holder, item)
            }
        })
        viewLifecycleOwner.lifecycleScope.launch {
            if (isAdded) {
                launch {
                    binding.bindViews()
                    getStoredPackages()
                }.join()
                binding.progressBar.visibility = ProgressBar.INVISIBLE
                restoreAdapter.submitList(applicationList)
            }
        }
    }

    private fun FragmentRestoreListBinding.bindViews() {
        viewLifecycleOwner.lifecycleScope.launch {
            bindToolBar()
            bindRecyclerView()
            bindSwipeContainer()
            bindFloatingButton()
        }
    }

    private fun FragmentRestoreListBinding.bindSwipeContainer() {
        swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val refresh = launch {
                    getStoredPackages()
                }
                refresh.join()
                swipeRefresh.isRefreshing = false
                delay(250)
                restoreAdapter.submitList(applicationList)
            }
        }
    }

    private fun FragmentRestoreListBinding.bindRecyclerView() {
        restoreRecyclerView.apply {
            adapter = restoreAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(5)
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
                            JsonUtil.deserializeApp(jsonFile).collect { app ->
                                tempApps.add(app)
                            }
                        }
                    }
                    applicationList.clear()
                    applicationList.addAll(tempApps)
                    applicationList.sortBy { it.name }
                } else {
                    FileUtil.createDirectory(path)
                    FileUtil.createFile("$path/.nomedia")
                }
            }
        }
    }

    private fun FragmentRestoreListBinding.bindToolBar() {
        toolBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    private fun FragmentRestoreListBinding.bindFloatingButton() {
        floatingButton.hide()
        hideButton()

        floatingButton.setOnClickListener {
            restoreRecyclerView.smoothScrollToPosition(0)
        }
    }

    private fun FragmentRestoreListBinding.hideButton() {
        restoreRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _restoreAdapter = null
    }
}