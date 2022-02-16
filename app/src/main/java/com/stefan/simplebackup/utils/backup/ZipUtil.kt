package com.stefan.simplebackup.utils.backup

import android.util.Log
import com.stefan.simplebackup.data.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

class ZipUtil(private val deserializedApps: HashMap<AppData, String>) {

    suspend fun zipContainer() {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                deserializedApps.map { entry ->
                    entry.value
                }.forEach { backupDirPath ->
                    val backupDir = File(backupDirPath)
                    var containerFilePath = ""
                    backupDir.listFiles()?.filter { dataFile ->
                        dataFile.isFile && dataFile.extension == "tar"
                    }?.map {
                        containerFilePath = it.absolutePath
                    }

                    val zipParameters = ZipParameters()
                    zipParameters.apply {
                        isEncryptFiles = true
                        compressionMethod = CompressionMethod.DEFLATE
                        compressionLevel = CompressionLevel.FASTEST
                        encryptionMethod = EncryptionMethod.AES
                        aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    }

                    val zipContainer = File(containerFilePath)
                    val zipFile = ZipFile("$backupDirPath/data.zip", "pass123".toCharArray())
                    zipFile.addFile(zipContainer, zipParameters)
                    zipContainer.delete()
                }
            }
        }
    }

    suspend fun zipApk() {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val zipParameters = ZipParameters()
                zipParameters.compressionMethod = CompressionMethod.STORE
                deserializedApps.forEach { entry ->
                    val app = entry.key
                    val backupDirPath = entry.value
                    val zipFile = ZipFile("$backupDirPath/apk.zip")
                    zipFile.addFiles(getApkList(app), zipParameters)
                    Log.d("ZipUtil", "Zipping apks to $backupDirPath")
                }
            }
        }
    }

    private fun getApkList(app: AppData): MutableList<File> {
        val apkList = mutableListOf<File>()
        Log.d("ZipUtil", "Found the ${app.getName()} apk dir: ${app.getApkDir()}")
        val dir = File(app.getApkDir())
        dir.walkTopDown().filter {
            it.extension == "apk"
        }.forEach { apk ->
            apkList.add(apk)
        }
        Log.d("ZipUtil", "Got the apk list for ${app.getName()}: ${apkList.map { it.name }}")
        return apkList
    }
}