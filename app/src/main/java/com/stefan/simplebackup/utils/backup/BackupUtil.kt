package com.stefan.simplebackup.utils.backup

import android.content.Context
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.utils.archive.TarUtil
import com.stefan.simplebackup.utils.archive.ZipUtil
import com.stefan.simplebackup.utils.main.PreferenceHelper
import com.stefan.simplebackup.utils.main.ioDispatcher
import com.stefan.simplebackup.workers.PROGRESS_MAX
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(
    private val appContext: Context,
    private val packageNames: Array<String>
) : BackupHelper(appContext) {

    private var zipUtil: ZipUtil? = null
    private var tarUtil: TarUtil? = null

    private val mainApplication: MainApplication = appContext as MainApplication
    private val repository = mainApplication.getRepository

    private var currentProgress = 0
    private val updatedProgress: Int
        get() {
            currentProgress += (PROGRESS_MAX / packageNames.size) / 3
            return currentProgress
        }

    suspend fun backup() = flow {
        prepareUtils()
        packageNames.forEach { packageName ->
            repository.getAppByPackageName(packageName).also { app ->
                savePackageNameToPreferences(packageName)
                emitProgressData(createDirs(app))
                emitProgressData(backupData(app))
                emitProgressData(zipData(app))
                outputAppInfo(app)
            }
        }
    }

    private suspend fun savePackageNameToPreferences(packageName: String) {
        withContext(ioDispatcher) {
            PreferenceHelper.savePackageName(packageName)
        }
    }

    private suspend fun prepareUtils() {
        if (zipUtil == null && tarUtil == null) {
            val app = repository.getAppByPackageName(packageNames.first())
            zipUtil = ZipUtil(appContext, app)
            tarUtil = TarUtil(appContext, app)
        }
    }

    private suspend fun createDirs(app: AppData): Flow<Pair<Int, AppData>> {
        createMainDir()
        createAppBackupDir(getBackupDirPath(app))
        return flowOf(updatedProgress to app)
    }

    private suspend fun backupData(app: AppData): Flow<Pair<Int, AppData>> {
        if (tarUtil?.app != app) {
            tarUtil?.app = app
        }
        tarUtil?.backupData()
        return flowOf(updatedProgress to app)
    }

    private suspend fun zipData(app: AppData): Flow<Pair<Int, AppData>> {
        if (zipUtil?.app != app) {
            zipUtil?.app = app
        }
        zipUtil?.zipAllData()
        return flowOf(updatedProgress to app)
    }

    private suspend fun outputAppInfo(app: AppData) {
        setBackupTime(app)
        serializeApp(app)
    }

    private suspend fun <T> FlowCollector<T>.emitProgressData(flow: Flow<T>) {
        this.emitAll(flow)
    }
}