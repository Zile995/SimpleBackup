package com.stefan.simplebackup.restore

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.adapter.RestoreAdapter
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import com.stefan.simplebackup.databinding.ActivityRestoreBinding
import com.stefan.simplebackup.helper.SearchHelper
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

class RestoreActivity : AppCompatActivity() {

    private var applicationList = mutableListOf<Application>()
    private var bitmapList = mutableListOf<ApplicationBitmap>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var topBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var restoreAdapter: RestoreAdapter
    private lateinit var floatingButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)

        val binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createTopBar(binding)
        createFloatingButton(binding)
        createSwipeContainer(binding)
        createRecyclerView(binding)
        hideButton(recyclerView)

        CoroutineScope(Dispatchers.Main).launch {
            launch {
                getStoredPackages()
            }.join()
            launch {
                updateAdapter()
            }
        }

        floatingButton.setOnClickListener {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(0, 0)
        }

        swipeContainer.setOnRefreshListener {
            CoroutineScope(Dispatchers.Main).launch {
                val refresh = launch {
                    refreshStoredPackages()
                    // Delay kako bi potrajala swipe refresh animacija
                    delay(400)
                }
                refresh.join()
                launch {
                    swipeContainer.isRefreshing = false
                }
                launch {
                    delay(200)
                    updateAdapter()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_restore_bar, menu)
        val menuItem = menu?.findItem(R.id.search)
        val searchView = menuItem?.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.queryHint = "Search for apps"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                SearchHelper.search(applicationList, bitmapList, this@RestoreActivity, newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun createSwipeContainer(binding: ActivityRestoreBinding) {
        swipeContainer = binding.swipeRefresh
    }

    private fun createRecyclerView(binding: ActivityRestoreBinding) {
        recyclerView = binding.restoreRecyclerView
        restoreAdapter = RestoreAdapter(this)
        recyclerView.adapter = restoreAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun updateAdapter() {
        restoreAdapter.updateList(applicationList, bitmapList, this)
    }

    private suspend fun refreshStoredPackages() {
        withContext(Dispatchers.Default) {
            getStoredPackages()
        }
    }

    private fun getStoredPackages() {
        val tempAppList = mutableListOf<Application>()
        val tempBitmapList = mutableListOf<ApplicationBitmap>()
        val path = "/storage/emulated/0/SimpleBackup"
        val dir = File(path)
        if (dir.exists()) {
            val listFiles = dir.listFiles()

            if (!listFiles.isNullOrEmpty()) {
                listFiles.forEach {
                    Log.d("listfiles", it.toString())
                    if (it.isDirectory) {
                        it.listFiles()?.forEach { dir ->
                            if (dir.absolutePath.contains(".json")) {
                                File(dir.absolutePath).inputStream().bufferedReader()
                                    .use { reader ->
                                        val string = reader.readLine()
                                        Log.d("asdf", string)
                                        tempAppList.add(
                                            Json.decodeFromString(string)
                                        )
                                    }
                            } else if (dir.absolutePath.contains(".png")) {
                                val stringBitmap = dir.name.substring(dir.name.lastIndexOf('/') + 1)
                                    .removeSuffix(".png")
                                val bitmap = BitmapFactory.decodeStream(
                                    FileInputStream(
                                        File(dir.absolutePath)
                                    )
                                )
                                Log.d("bitmap", bitmap.toString())
                                Log.d("stringBitmap", stringBitmap)
                                tempBitmapList.add(
                                    ApplicationBitmap(
                                        stringBitmap,
                                        bitmap
                                    )
                                )
                            }
                        }
                    }
                }
            }
            applicationList = tempAppList.sortedBy { it.getName() } as MutableList<Application>
            bitmapList = tempBitmapList.sortedBy { it.getName() } as MutableList<ApplicationBitmap>
        }
    }

    fun getAdapter(): RestoreAdapter {
        return restoreAdapter
    }

    private fun createTopBar(binding: ActivityRestoreBinding) {
        topBar = binding.topAppBar
        topBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(topBar)
    }

    private fun createFloatingButton(binding: ActivityRestoreBinding) {
        floatingButton = binding.floatingButton
    }

    private fun hideButton(recyclerView: RecyclerView?) {
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0 && floatingButton.isShown) {
                    floatingButton.hide()
                } else if (dy < 0 && !floatingButton.isShown) {
                    floatingButton.show()
                }
            }
        })
    }
}