package com.stefan.simplebackup.utils.root

import android.content.Context
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.work.APK_FILE_EXTENSION
import com.stefan.simplebackup.utils.work.FileUtil
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val APK_TMP_INSTALL_DIR_PATH = "/data/local/tmp"

class RootApkManager(private val context: Context) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val packageInstaller = context.packageManager.packageInstaller

    suspend fun installApk(apkDirPath: String) {
        withContext(ioDispatcher) {
            val apkSizeMap = HashMap<File, Long>()
            File(apkDirPath).walkTopDown().filter {
                it.isFile && it.extension == APK_FILE_EXTENSION
            }.sumOf { apkFile ->
                apkSizeMap[apkFile] = apkFile.length()
                apkFile.length()
            }.also { totalApkSize ->
                installSplitApks(apkSizeMap, totalApkSize)
            }
        }
    }

    @Throws(IOException::class)
    suspend fun createTempInstallDir(app: AppData) {
        withContext(ioDispatcher) {
            val tempApkInstallDirPath = getTempApkInstallDirPath(app)
            val result = Shell.cmd("mkdir -p $tempApkInstallDirPath").exec()
            if (!result.isSuccess) {
                throw IOException("Unable to create temp apk install dir")
            }
        }
    }

    @Throws(IOException::class)
    suspend fun deleteTempInstallDir(app: AppData) {
        withContext(ioDispatcher) {
            val tempApkInstallDirPath = getTempApkInstallDirPath(app)
            val result = Shell.cmd("rm -rf $tempApkInstallDirPath").exec()
            if (!result.isSuccess) {
                throw IOException("Unable to delete temp apk install dir")
            }
        }
    }

    @Throws(IOException::class)
    suspend fun moveApkFilesToTempDir(app: AppData) {
        withContext(ioDispatcher) {
            val tempApkDir = FileUtil.getTempDirPath(app)
            val tempApkInstallDirPath = getTempApkInstallDirPath(app)
            val result =
                Shell.cmd("mv $tempApkDir/*.$APK_FILE_EXTENSION $tempApkInstallDirPath/").exec()
            if (!result.isSuccess) {
                deleteTempInstallDir(app)
                throw IOException("Unable to move apk's to temp install dir")
            }
        }
    }

    fun getTempApkInstallDirPath(app: AppData): String =
        "$APK_TMP_INSTALL_DIR_PATH/${app.packageName}"

    @Throws(IOException::class)
    suspend fun uninstallApk(packageName: String) {
        withContext(ioDispatcher) {
            val result = Shell.cmd("pm uninstall $packageName").exec()
            if (!result.isSuccess) throw IOException("Unable to uninstall app")
        }
    }

    private fun createAndGetSessionId(totalApkSize: Long): Int {
        // Create installer session
        Shell.cmd("pm install-create -i ${context.packageName} --user current -r -t -S $totalApkSize")
            .exec()
        // Return first session id
        return packageInstaller.allSessions[0].sessionId
    }

    private fun commitSession(sessionId: Int) =
        Shell.cmd("pm install-commit $sessionId").exec()

    private fun installSplitApk(apkFile: File, apkSize: Long, sessionId: Int) =
        Shell.cmd("pm install-write -S $apkSize $sessionId ${apkFile.name} ${apkFile.absolutePath}")
            .exec()

    @Throws(IOException::class)
    private fun installSplitApks(apkSizeMap: HashMap<File, Long>, totalApkSize: Long) {
        val sessionId = createAndGetSessionId(totalApkSize = totalApkSize)
        for ((apkFile, apkSize) in apkSizeMap) {
            installSplitApk(apkFile, apkSize, sessionId)
        }
        val result = commitSession(sessionId)
        if (!result.isSuccess) {
            throw IOException("Unable to install app")
        }
    }

    companion object {
        fun unsuspendPackages(packageNames: Array<String>) {
            packageNames.forEach { packageName ->
                unsuspendPackage(packageName)
            }
        }

        fun suspendPackage(packageName: String) {
            Shell.cmd("am force-stop $packageName").exec()
            Shell.cmd("am kill $packageName").exec()
            Shell.cmd("cmd package suspend $packageName").exec()
        }

        fun unsuspendPackage(packageName: String) {
            Shell.cmd("am kill $packageName").exec()
            Shell.cmd("cmd package unsuspend $packageName").exec()
        }
    }
}