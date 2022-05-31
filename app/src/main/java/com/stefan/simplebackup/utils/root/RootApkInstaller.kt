package com.stefan.simplebackup.utils.root

import android.content.Context
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.withContext
import java.io.File

class RootApkInstaller(context: Context) {

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
        Shell.su("pm install-create -S $totalSize").exec()
        val sessionId = packageInstaller
            .allSessions[0]
            .sessionId
        for ((apk, apkSize) in apkSizeMap) {
            val apkFilePath = "x=$(echo -e \"${apk.absolutePath}\")"
            Shell.su(
                apkFilePath +
                        " && pm install-write -S $apkSize $sessionId ${apk.name} " +
                        "\"\$x\""
            ).exec()
        }
        Shell.su("pm install-commit $sessionId").exec()
    }
}