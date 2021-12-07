package com.stefan.simplebackup

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
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
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stefan.simplebackup.adapter.AppAdapter
import com.stefan.simplebackup.data.AppInfo
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.restore.RestoreActivity
import com.stefan.simplebackup.shell.SplashActivity
import com.stefan.simplebackup.utils.PermissionUtils
import com.stefan.simplebackup.utils.RootChecker
import com.stefan.simplebackup.utils.SearchUtil
import kotlinx.coroutines.*
import java.util.*

open class MainActivity : AppCompatActivity() {

    // Const values
    companion object {
        private const val STORAGE_PERMISSION_CODE: Int = 500
    }

    private var PACKAGE_NAME: String = ""
    private var rootChecker = RootChecker(this)
    private var scope = CoroutineScope(Job() + Dispatchers.Main)


    private var applicationList = AppInfo.getUserAppList

    // UI
    private lateinit var toolBar: Toolbar
    private lateinit var swipeContainer: SwipeRefreshLayout
    private lateinit var appAdapter: AppAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var floatingButton: FloatingActionButton
    private lateinit var bottomBar: BottomNavigationView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipFilter: Chip

    // PackageManager
    private lateinit var pm: PackageManager

    // Flags
    private val flags: Int = PackageManager.GET_META_DATA
    private var isSubmitted: Boolean = false

    /**
     * - Standardna onCreate metoda Activity Lifecycle-a
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Postavi View Binding
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            // Pokupi sačuvano informaciju o tome da li je postavljen root upit.
            isSubmitted = savedInstanceState.getBoolean("isSubmitted");
        }

        val rootSharedPref =
            this@MainActivity.getSharedPreferences("root_access", MODE_PRIVATE)

        PACKAGE_NAME = this.applicationContext.packageName
        pm = AppInfo.getPackageManager

        with(this) {
            window.setBackgroundDrawableResource(R.color.background)
            window.statusBarColor = getColor(R.color.bottom_bar)
        }

        scope.launch {
            launch {
                // Inicijalizuj sve potrebne UI elemente redom
                bindViews(binding)
            }
            val load = launch {
                applicationList = SplashActivity.result.await()
            }
            launch {
                if (!AppInfo.databaseExists(this@MainActivity)) {
                    AppInfo.makeDatabase()
                }
            }
            load.join()
            val set = launch {
                progressBar.visibility = View.GONE
                updateAdapter()
            }
            set.join()
            launch {
                delay(250)
                if (!isSubmitted) {
                    // Ostavićemo da Magisk prikazuje Toast kao obaveštenje da nemamo root access
                    // Prikazuj kada se svaki put pozove onCreate metoda
                    checkForRoot(rootSharedPref)
                    isSubmitted = true
                }
                if (!rootSharedPref.getBoolean(
                        "checked",
                        false
                    ) && rootChecker.isRooted() && !rootSharedPref.getBoolean("root_granted", false)
                ) {
                    rootDialog(
                        false,
                        getString(R.string.root_detected),
                        getString(R.string.not_granted)
                    )
                } else if (!rootSharedPref.getBoolean(
                        "checked",
                        false
                    ) && !rootChecker.isRooted()
                ) {
                    rootDialog(
                        false,
                        getString(R.string.not_rooted),
                        getString(R.string.not_rooted_info)
                    )
                }
            }
        }
    }

    private fun bindViews(binding: ActivityMainBinding) {
        with(binding) {
            createProgressBar(this)
            createToolBar(this)
            createSwipeContainer(this)
            createRecyclerView(this)
            createFloatingButton(this)
            createChipFilter(this)
            createBottomBar(this)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isSubmitted", isSubmitted)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        if (!checkPermission()) {
            requestPermission()
        }
        super.onResume()
    }

    private fun checkForRoot(rootSharedPref: SharedPreferences) {
        if (rootChecker.hasRootAccess()) {
            rootSharedPref.edit().putBoolean("root_granted", true).apply()
        } else {
            rootSharedPref.edit().putBoolean("root_granted", false).apply()
        }
    }

    private fun checkPermission(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (PermissionUtils.neverAskAgainSelected(
                this,
                WRITE_EXTERNAL_STORAGE
            )
        ) {
            permissionDialog()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(WRITE_EXTERNAL_STORAGE),
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
                            getString(R.string.storage_perm_success),
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
                if (applicationList.size > 0) {
                    SearchUtil.search(applicationList, this@MainActivity, newText)
                }
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    private fun permissionDialog() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString(R.string.storage_permission))
            .setMessage(getString(R.string.storage_perm_info))
            .setPositiveButton(getString(R.string.set_manually)) { _, _ ->
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val uri = Uri.parse("package:" + PACKAGE_NAME)
                intent.setData(uri)
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                Process.killProcess(Process.myPid())
            }
            .setCancelable(false)
        val alert = builder.create()
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.red))
            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.blue))
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
            this.getSharedPreferences("root_access", MODE_PRIVATE).edit()
                .putBoolean("checked", true).apply()
            val alert = builder.create()
            alert.setOnShowListener {
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(resources.getColor(R.color.blue))
            }
            alert.show()
        }
    }

    /**
     * - Inicijalizuj gornju traku, ili ToolBar
     */
    private fun createToolBar(binding: ActivityMainBinding) {
        toolBar = binding.toolBar
        toolBar.setTitleTextAppearance(this, R.style.ActionBarTextAppearance)
        setSupportActionBar(toolBar)
    }

    private fun createProgressBar(binding: ActivityMainBinding) {
        progressBar = binding.progressBar
    }

    /**
     * - Inicijalizuj recycler view
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun createRecyclerView(
        binding: ActivityMainBinding
    ) {
        recyclerView = binding.recyclerView
        appAdapter = AppAdapter(this)
        recyclerView.adapter = appAdapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun createSwipeContainer(binding: ActivityMainBinding) {
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
    private fun createFloatingButton(binding: ActivityMainBinding) {
        floatingButton = binding.floatingButton
        floatingButton.hide()
        hideButton(recyclerView)

        floatingButton.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    private fun createChipFilter(binding: ActivityMainBinding) {
        chipFilter = binding.chipFilter
        chipFilter.isFocusable = true
        chipFilter.isCheckable = false
        chipFilter.visibility = View.INVISIBLE
    }

    /**
     * - Inicijalizuj donju navigacionu traku
     */
    private fun createBottomBar(binding: ActivityMainBinding) {
        bottomBar = binding.bottomNavigation

        bottomBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.restore_local -> {
                    val intent = Intent(this, RestoreActivity::class.java)
                    startActivity(intent)
                }
            }
            true
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

    /**
     *  - Prosleđuje AppAdapter adapteru novu listu i obaveštava RecyclerView da je lista promenjena
     */
    private suspend fun refreshPackageList() {
        withContext(Dispatchers.IO) {
            launch {
                AppInfo.getInstalledApplications(flags).setPackageList(this@MainActivity)
            }
        }
    }

    private fun updateAdapter() {
        appAdapter.updateList(applicationList)
    }

    fun getAdapter(): AppAdapter {
        return appAdapter
    }
}