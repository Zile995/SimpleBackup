package com.stefan.simplebackup

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.stefan.simplebackup.utils.PermissionUtils
import com.stefan.simplebackup.utils.RootChecker
import com.stefan.simplebackup.utils.SearchUtil
import kotlinx.coroutines.*
import java.io.File
import java.util.*


open class MainActivity : AppCompatActivity() {

    companion object {
        private const val STORAGE_PERMISSION_CODE: Int = 500
    }

    private var PACKAGE_NAME: String = ""
    private var rootChecker = RootChecker(this)
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

        PACKAGE_NAME = this.applicationContext.packageName
        pm = packageManager
        val rootSharedPref =
            this@MainActivity.getSharedPreferences("root_access", Context.MODE_PRIVATE)
        val rootChecked =
            this@MainActivity.getSharedPreferences("root_checked", Context.MODE_PRIVATE)

        // Inicijalizuj sve potrebne elemente redom
        createProgressBar(binding)
        createTopBar(binding)
        createSwipeContainer(binding)
        createRecyclerView(binding)
        createFloatingButton(binding)
        createBottomBar(binding)
        hideButton(recyclerView)

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
                R.id.restore_local -> {
                    val intent = Intent(this, RestoreActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (rootChecker.hasRootAccess()) {
                rootChecked.edit().putBoolean("checked", true).apply()
                rootSharedPref.edit().putBoolean("root_granted", true).apply()
            } else {
                rootSharedPref.edit().putBoolean("root_granted", false).apply()
            }
        }
    }

    override fun onResume() {
        scope.launch {
            val permission = launch {
                if (!checkPermission()) {
                    requestPermission()
                }
            }
            permission.join()
            launch {
                val rootSharedPref =
                    this@MainActivity.getSharedPreferences("root_access", Context.MODE_PRIVATE)
                val rootChecked =
                    this@MainActivity.getSharedPreferences("root_checked", Context.MODE_PRIVATE)
                if (rootChecker.isRooted(true)) {
                    Log.d("root", "Phone is rooted")
                    if (!rootSharedPref.getBoolean("root_granted", true)) {
                        Log.d("access", "Phone doesn't have root access")
                        rootDialog(
                            rootChecked.getBoolean("checked", false),
                            "Root detected!",
                            "But you did not grant root access\nRoot access is needed, in order to backup application data"
                        )
                    }
                } else {
                    Log.d("root", "Phone is not rooted")
                    rootDialog(
                        rootChecked.getBoolean("checked", false),
                        "Your phone is not rooted",
                        "However, you will only be able to back up the apk without app data"
                    )

                }
            }
        }
        super.onResume()
    }

    private fun checkPermission(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (PermissionUtils.neverAskAgainSelected(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            permissionDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.size > 0) {
                    val WRITE_EXTERNAL_STORAGE =
                        grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (WRITE_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Permission granted successfully",
                            Toast.LENGTH_LONG
                        ).show();
                    } else {
                        PermissionUtils.setShowDialog(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }
                }
            }
            else -> {
                throw Exception("Wrong request code")
            }
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
                SearchUtil.search(applicationList, bitmapList, this@MainActivity, newText)
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun permissionDialog() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle("Permission not granted!")
            .setMessage("This permission is required to manage backup files and folders.\nPlease set the storage permission.")
            .setPositiveButton("Set manually") { _, _ ->
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri = Uri.parse("package:" + PACKAGE_NAME)
                intent.setData(uri)
                startActivity(intent)
            }
            .setCancelable(false)
        val alert = builder.create()
        alert.setOnShowListener{
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.red))
        }
        alert.show()
    }

    private fun rootDialog(checked: Boolean, title: String, message: String) {
        if (!checked) {
            val builder = AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.OK)) { dialog, _ ->
                    dialog.cancel()
                }
            this.getSharedPreferences("root_checked", Context.MODE_PRIVATE).edit()
                .putBoolean("checked", true).apply()
            val alert = builder.create()
            alert.setOnShowListener{
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(resources.getColor(R.color.blue))
            }
            alert.show()
        }
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
                val apkDir = it.publicSourceDir.removeSuffix("/base.apk")
                tempApps.add(
                    Application(
                        it.loadLabel(pm).toString(),
                        it.packageName,
                        packageInfoList[index].versionName,
                        it.dataDir,
                        apkDir,
                        "",
                        getApkSize(apkDir)
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

    private fun getApkSize(path: String): Long {
        val dir = File(path)
        return dir.walkTopDown().filter {
            it.absolutePath.contains("apk")
        }.map {
            it.length()
        }.sum()
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