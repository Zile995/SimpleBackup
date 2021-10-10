package com.stefan.simplebackup.restore

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.R
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import com.stefan.simplebackup.databinding.ActivityRestoreBinding
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream

class RestoreActivity : AppCompatActivity() {
    private var applicationList = mutableListOf<Application>()
    private var bitmapList = mutableListOf<ApplicationBitmap>()
    private var recyclerView: RecyclerView? = null

    private lateinit var topBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var appAdapter: AppAdapter
    private lateinit var floatingButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)

        val binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createTopBar(binding)
        createFloatingButton(binding)
        hideButton(recyclerView)

        createRecyclerView(binding)
    }

    private fun createRecyclerView(binding: ActivityRestoreBinding) {
        CoroutineScope(Dispatchers.Main).launch {
            swipeContainer = binding.swipeRefresh
            recyclerView = binding.recyclerView

            // Postavi LinearLayoutManager layoutManager za recyclerView
            recyclerView?.setHasFixedSize(true)
            recyclerView?.layoutManager = LinearLayoutManager(this@RestoreActivity)

            // Dobavi novu listu i prosledi konstruktoru AppAdaptera
            val result = async { getStoredPackages() }
            if (result.await()) {
                applicationList = applicationList.sortedBy { it.getName() } as MutableList<Application>
                bitmapList = bitmapList.sortedBy { it.getName() } as MutableList<ApplicationBitmap>
                appAdapter = AppAdapter(applicationList, bitmapList, false)
                recyclerView?.adapter = appAdapter
            }

        }
    }

    private fun getStoredPackages(): Boolean {
            val path = "/storage/emulated/0/SimpleBackup"
            val dir = File(path)
            val listFiles = dir.listFiles()

            if (listFiles != null && listFiles.isNotEmpty()) {
                listFiles.forEach {
                    Log.d("listfiles", it.toString())
                    if (it.isDirectory) {
                        it.listFiles()?.forEach { dir ->
                            if (dir.toString().contains(".json")) {
                                File(dir.toString()).inputStream().bufferedReader().use { reader ->
                                    val string = reader.readLine()
                                    Log.d("asdf", string)
                                    applicationList.add(
                                        Json.decodeFromString(string)
                                    )
                                }
                            } else if (dir.toString().contains(".png")) {
                                val stringBitmap = dir.name.substring(dir.name.lastIndexOf('/') + 1)
                                    .removeSuffix(".png")
                                val pathBitmap = dir.path.removeSuffix(dir.name)
                                val bitmap = BitmapFactory.decodeStream(
                                    FileInputStream(
                                        File(
                                            pathBitmap,
                                            "$stringBitmap.png"
                                        )
                                    )
                                )
                                Log.d("bitmap", bitmap.toString())
                                Log.d("stringBitmap", stringBitmap)
                                bitmapList.add(
                                    ApplicationBitmap(
                                        stringBitmap,
                                        bitmap
                                    )
                                )
                            }
                        }
                    }
                }
                return true
            } else return false
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