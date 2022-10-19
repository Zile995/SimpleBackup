package com.stefan.simplebackup.utils.root

import android.content.Context
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.file.APK_FILE_EXTENSION
import com.stefan.simplebackup.utils.file.FileUtil
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class RootApkManager(context: Context) {

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
            val tempApkInstallDirPath = FileUtil.getTempApkInstallDirPath(app)
            val result = Shell.cmd("mkdir -p $tempApkInstallDirPath").exec()
            if (!result.isSuccess) {
                throw IOException("Unable to create temp install dirs")
            }
        }
    }

    @Throws(IOException::class)
    suspend fun deleteTempInstallDir(app: AppData) {
        withContext(ioDispatcher) {
            val tempApkInstallDirPath = FileUtil.getTempApkInstallDirPath(app)
            val result = Shell.cmd("rm -rf $tempApkInstallDirPath").exec()
            if (!result.isSuccess) {
                throw IOException("Unable to delete temp install dirs")
            }
        }
    }

    @Throws(IOException::class)
    suspend fun moveApkFilesToTempDir(app: AppData) {
        withContext(ioDispatcher) {
            val tempApkDir = FileUtil.getTempDirPath(app)
            val tempApkInstallDirPath = FileUtil.getTempApkInstallDirPath(app)
            val result = Shell.cmd("mv $tempApkDir/*.$APK_FILE_EXTENSION $tempApkInstallDirPath/").exec()
            if (!result.isSuccess) {
                deleteTempInstallDir(app)
                throw IOException("Unable to move apk's")
            }
        }
    }

    @Throws(IOException::class)
    suspend fun uninstallApk(packageName: String) {
        withContext(ioDispatcher) {
            val result = Shell.cmd("pm uninstall $packageName").exec()
            if (!result.isSuccess) throw IOException("Unable to uninstall app")
        }
    }

    private fun createAndGetSessionId(totalApkSize: Long): Int {
        // Create installer session
        Shell.cmd("pm install-create -S $totalApkSize").exec()
        // Return first session id
        return packageInstaller.allSessions[0].sessionId
    }

    private fun commitSession(sessionId: Int) =
        Shell.cmd("pm install-commit $sessionId").exec()

    private fun installSplitApk(apkFile: File, apkSize: Long, sessionId: Int) =
        Shell.cmd("pm install-write -S $apkSize $sessionId ${apkFile.name} ${apkFile.absolutePath}")
            .exec()

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
}