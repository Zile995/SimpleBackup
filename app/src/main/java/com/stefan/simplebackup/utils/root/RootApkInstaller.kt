package com.stefan.simplebackup.utils.root

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RootApkInstaller(context: Context) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val packageInstaller = context.packageManager.packageInstaller

    suspend fun installApk(apkFolderPath: String) {
        withContext(ioDispatcher) {
            val apkSizeMap = HashMap<File, Long>()
            File(apkFolderPath).listFiles()?.filter {
                it.isFile && it.name.endsWith(".apk")
            }?.sumOf { apkFile ->
                apkSizeMap[apkFile] = apkFile.length()
                apkFile.length()
            }?.let { totalSize ->
                startInstalling(apkSizeMap, totalSize)
            }
        }
    }

    private fun startInstalling(apkSizeMap: HashMap<File, Long>, totalSize: Long) {
        Shell.cmd("pm install-create -S $totalSize").exec()
        val sessionId = packageInstaller
            .allSessions[0]
            .sessionId
        for ((apk, apkSize) in apkSizeMap) {
            val apkFilePath = "x=$(echo -e \"${apk.absolutePath}\")"
            Shell.cmd(
                apkFilePath +
                        " && pm install-write -S $apkSize $sessionId ${apk.name} " +
                        "\"\$x\""
            ).exec()
        }
        Shell.cmd("pm install-commit $sessionId").exec()
    }
}