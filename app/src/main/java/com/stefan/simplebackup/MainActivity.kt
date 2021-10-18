package com.stefan.simplebackup

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.Application
import com.stefan.simplebackup.data.ApplicationBitmap
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.helper.SearchHelper
import com.stefan.simplebackup.restore.RestoreActivity
import kotlinx.coroutines.*
import java.io.File

open class MainActivity : AppCompatActivity() {

    private var PACKAGE_NAME: String? = null
    private var scope = CoroutineScope(Dispatchers.Main)

    private lateinit var applicationList: MutableList<Application>
    private lateinit var bitmapList: MutableList<ApplicationBitmap>
    private lateinit var topBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var appAdapter: AppAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingButton: FloatingActionButton
    private lateinit var bottomBar: BottomNavigationView
    private lateinit var progressBar: ProgressBar

    private lateinit var pm: PackageManager
    private lateinit var applicationInfoList: MutableList<ApplicationInfo>
    private lateinit var packageInfoList: MutableList<PackageInfo>

    private val flags: Int = PackageManager.GET_META_DATA or
            PackageManager.GET_SHARED_LIBRARY_FILES

    /**
     * - Standardna onCreate metoda Activity Lifecycle-a
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Postavi View Binding
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicijalizuj package varijable
        PACKAGE_NAME = this.applicationContext.packageName
        pm = packageManager

        // Inicijalizuj sve potrebne elemente redom
        createProgressBar(binding)
        createTopBar(binding)
        createSwipeContainer(binding)
        createRecyclerView(binding)
        createFloatingButton(binding)
        createBottomBar(binding)

        CoroutineScope(Dispatchers.Main).launch {
            val load = launch {
                refreshPackageList()
            }
            load.join()
            launch {
                progressBar.visibility = View.GONE
                delay(500)
                updateAdapter()
                hideButton(recyclerView)
            }
        }

        //Postavi sve potrebne Listener-e
        swipeContainer.setOnRefreshListener {
            CoroutineScope(Dispatchers.Main).launch {
                val refresh = launch {
                    refreshPackageList()
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

        floatingButton.setOnClickListener {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(0, 0)
        }

        bottomBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.more -> {
                    val intent = Intent(this, RestoreActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }

    }

    /**
     * - Kreiraj menu i podesi listener za search polje
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        val menuItem = menu?.findItem(R.id.search)
        val searchView = menuItem?.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.queryHint = "Search for apps"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                SearchHelper.search(applicationList, bitmapList, this@MainActivity, newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * - Inicijalizuj gornju traku, ili ToolBar
     */
    private fun createTopBar(binding: ActivityMainBinding) {
        topBar = binding.topAppBar
        topBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(topBar)
    }

    private fun createProgressBar(binding: ActivityMainBinding) {
        progressBar = binding.progressBar
        progressBar.visibility = View.VISIBLE
    }

    /**
     * - Inicijalizuj recycler view
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun createRecyclerView(
        binding: ActivityMainBinding
    ) {
        recyclerView = binding.recyclerView
        appAdapter = AppAdapter()
        recyclerView.adapter = appAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun createSwipeContainer(binding: ActivityMainBinding) {
        swipeContainer = binding.swipeRefresh
    }

    /**
     *  - Prosleđuje AppAdapter adapteru novu listu i obaveštava RecyclerView da je lista promenjena
     */
    private suspend fun refreshPackageList() {
        withContext(Dispatchers.Default) {
            getPackageInfo()
        }
    }

    private fun getPackageInfo() {
        applicationInfoList = pm.getInstalledApplications(flags)
        packageInfoList = pm.getInstalledPackages(0)
        applicationList = getPackageList(applicationInfoList, packageInfoList, pm, flags)
        bitmapList = getBitmapList(applicationInfoList, pm)
    }

    private fun updateAdapter() {
        appAdapter.updateList(applicationList, bitmapList)
    }

    /**
     * - Inicijalizuj Floating dugme
     */
    private fun createFloatingButton(binding: ActivityMainBinding) {
        floatingButton = binding.floatingButton
    }

    /**
     * - Inicijalizuj donju navigacionu traku
     */
    private fun createBottomBar(binding: ActivityMainBinding) {
        bottomBar = binding.bottomNavigation

    }

    fun getAdapter(): AppAdapter {
        return appAdapter
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
        })
    }

    /**
     *  - Osvežava listu aplikacija kada je Activity u fokusu
     *
     */
    override fun onResume() {
        CoroutineScope(Dispatchers.Main).launch {
            refreshPackageList()
            updateAdapter()
            topBar.collapseActionView()
        }
        super.onResume()
    }

    override fun onBackPressed() {
        topBar.collapseActionView()
        super.onBackPressed()
    }

    /**
     * - Puni MutableList sa izdvojenim objektima Application klase
     *
     * - pm je isntanca PackageManager klase pomoću koje dobavljamo sve informacije o aplikacijama
     *
     * - SuppressLint ignoriše upozorenja vezana za getInstalledApplications,
     *   jer Android 11 po defaultu ne prikazuje sve informacije instaliranih aplikacija.
     *   To se može zaobići u AndroidManifest.xml fajlu dodavanjem
     *   **<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
     *   tools:ignore="QueryAllPackagesPermission" />**
     */
    @SuppressLint("QueryPermissionsNeeded")
    private fun getPackageList(
        applicationInfoList: MutableList<ApplicationInfo>,
        packageInfoList: MutableList<PackageInfo>,
        pm: PackageManager,
        flags: Int
    ): MutableList<Application> {
        val list = mutableListOf<Application>()

        for (i in 0 until applicationInfoList.size) {
            if (isUserApp(applicationInfoList[i]) || applicationInfoList[i].packageName.equals(
                    PACKAGE_NAME
                )
            ) {
                // Preskoči sistemske aplikacije i moju aplikaciju
            } else {
                list.add(
                    Application(
                        applicationInfoList[i].loadLabel(pm).toString(),
                        applicationInfoList[i].packageName,
                        packageInfoList[i].versionName,
                        applicationInfoList[i].dataDir,
                        "",
                        File(
                            pm.getApplicationInfo(
                                applicationInfoList[i].packageName,
                                flags
                            ).sourceDir
                        ).length()
                    )
                )
            }
        }
        Log.d("return:", list.toString())

        return list.sortedBy { it.getName() } as MutableList<Application>
    }

    /**
     * - Puni MutableList sa izdvojenim objektima ApplicationBitmap klase
     */
    private fun getBitmapList(
        applicationInfoList: MutableList<ApplicationInfo>,
        pm: PackageManager
    ): MutableList<ApplicationBitmap> {
        val list = mutableListOf<ApplicationBitmap>()

        for (i in 0 until applicationInfoList.size) {
            if (isUserApp(applicationInfoList[i]) || applicationInfoList[i].packageName.equals(
                    PACKAGE_NAME
                )
            ) {
                // Preskoči sistemske aplikacije i moju aplikaciju
            } else {
                list.add(
                    ApplicationBitmap(
                        applicationInfoList[i].loadLabel(pm).toString(),
                        drawableToBitmap(applicationInfoList[i].loadIcon(pm))
                    )
                )
            }
        }
        Log.d("return:", list.toString())
        return list.sortedBy { it.getName() } as MutableList<ApplicationBitmap>
    }

    /**
     * - Proverava da li je prosleđena aplikacija system app
     */
    private fun isUserApp(pkgInfo: ApplicationInfo): Boolean {
        return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    /**
     * - Prebacuje drawable u bitmap da bi je kasnije skladištili na internu memoriju
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap: Bitmap

        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }
        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}