package com.stefan.simplebackup.restore

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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class RestoreActivity : AppCompatActivity() {

    private lateinit var applicationList: MutableList<Application>
    private lateinit var bitmapList: MutableList<ApplicationBitmap>
    private lateinit var topBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var appAdapter: AppAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore)

        val binding = ActivityRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createTopBar(binding)
        createFloatingButton(binding)

        recyclerView = binding.recyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        hideButton(recyclerView)

        getStoredPackages()
    }

    private fun getStoredPackages() {
        val path = "/storage/emulated/0/SimpleBackup"
        val dir = File(path)
        val listFiles = dir.listFiles()

        if (listFiles != null && listFiles.isNotEmpty()) {
            listFiles.forEach {
                if (it.isDirectory) {
                    it.listFiles()?.forEach { dir ->
                        if (dir.toString().contains(".txt")) {
                            File(dir.toString()).inputStream().bufferedReader().use { reader ->
                                val string = reader.readLine()
                                Log.d("asdf", string)
                                applicationList.add(
                                    Json.decodeFromString(string)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createTopBar(binding: ActivityRestoreBinding) {
        topBar = binding.topAppBar
        topBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(topBar)
    }

    private fun createFloatingButton(binding: ActivityRestoreBinding) {
        floatingButton = binding.floatingButton
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
        })
    }
}