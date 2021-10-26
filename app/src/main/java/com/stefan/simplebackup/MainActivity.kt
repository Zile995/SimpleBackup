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
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.Toast
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
import com.stefan.simplebackup.restore.RestoreActivity
import com.stefan.simplebackup.utils.RootChecker
import com.stefan.simplebackup.utils.SearchHelper
import kotlinx.coroutines.*
import java.io.File
import java.util.*

open class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_STORAGE: Int = 500
    }

    private var PACKAGE_NAME: String = ""
    private var scope = CoroutineScope(Job() + Dispatchers.Main)

    private var applicationList = mutableListOf<Application>()
    private var bitmapList = mutableListOf<ApplicationBitmap>()
    private var applicationInfoList = mutableListOf<ApplicationInfo>()
    private var packageInfoList = mutableListOf<PackageInfo>()

    private lateinit var topBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var appAdapter: AppAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingButton: FloatingActionButton
    private lateinit var bottomBar: BottomNavigationView
    private lateinit var progressBar: ProgressBar

    private lateinit var pm: PackageManager

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

//        if (rootChecker.isRooted(false)) {
//            Log.d("root", "Phone is rooted")
//            if (rootChecker.hasRootAccess()) {
//                Log.d("access", "Phone has root access")
//            } else {
//                Log.d("access", "Phone doesn't have root access")
//            }
//        } else {
//            Log.d("root", "Phone is not rooted")
//        }

        PACKAGE_NAME = this.applicationContext.packageName
        pm = packageManager

        // Inicijalizuj sve potrebne elemente redom
        createProgressBar(binding)
        createTopBar(binding)
        createSwipeContainer(binding)
        createRecyclerView(binding)
        createFloatingButton(binding)
        createBottomBar(binding)

        scope.launch {
            val load = launch {
                refreshPackageList()
                delay(250)
            }
            load.join()
            launch {
                progressBar.visibility = View.GONE
                delay(200)
                updateAdapter()
                hideButton(recyclerView)
            }
        }

        //Postavi sve potrebne Listener-e
        swipeContainer.setOnRefreshListener {
            scope.launch {
                val refresh = launch {
                    refreshPackageList()
                    // Delay kako bi potrajala swipe refresh animacija
                    delay(400)
                }
                refresh.join()
                launch {
                    swipeContainer.isRefreshing = false
                    delay(200)
                    updateAdapter()
                }
            }
        }

        floatingButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_STORAGE -> {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {

                }
            }
            else -> { throw Exception("Wrong request code") }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_STORAGE -> {
                if (grantResults.size > 0) {
                    val READ_EXTERNAL_STORAGE = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val WRITE_EXTERNAL_STORAGE = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (!READ_EXTERNAL_STORAGE && !WRITE_EXTERNAL_STORAGE) {
                        Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else -> { throw Exception("Wrong request code") }
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
        withContext(Dispatchers.IO) {
            launch {
                applicationInfoList = pm.getInstalledApplications(flags)
            }
            launch {
                packageInfoList = pm.getInstalledPackages(0)
            }.join()
            launch {
                getPackageList()
            }
        }
    }

    private fun updateAdapter() {
        appAdapter.updateList(applicationList, bitmapList)
    }

    /**
     * - Inicijalizuj Floating dugme
     */
    private fun createFloatingButton(binding: ActivityMainBinding) {
        floatingButton = binding.floatingButton
        floatingButton.hide()
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

    /**
     *  - Osvežava listu aplikacija kada je Activity u fokusu
     *
     */
    override fun onResume() {
        scope.launch {
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
    private fun getPackageList() {
        val tempApps = mutableListOf<Application>()
        val tempBitmaps = mutableListOf<ApplicationBitmap>()

        var index = 0
        applicationInfoList.forEach {
            if (isUserApp(it) || it.packageName.equals(
                    PACKAGE_NAME
                )
            ) {

            } else {
                tempApps.add(
                    Application(
                        it.loadLabel(pm).toString(),
                        it.packageName,
                        packageInfoList[index].versionName,
                        it.dataDir,
                        it.publicSourceDir.removeSuffix("/base.apk"),
                        "",
                        File(
                            pm.getApplicationInfo(
                                it.packageName,
                                flags
                            ).sourceDir
                        ).length()
                    )
                )
                tempBitmaps.add(
                    ApplicationBitmap(
                        it.loadLabel(pm).toString(),
                        drawableToBitmap(it.loadIcon(pm))
                    )
                )
            }
            index++
        }
        tempApps.sortBy { it.getName() }
        tempBitmaps.sortBy { it.getName() }
        applicationList = tempApps
        bitmapList = tempBitmaps
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