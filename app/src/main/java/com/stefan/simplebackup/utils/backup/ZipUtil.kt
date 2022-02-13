package com.stefan.simplebackup.utils.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

class ZipUtil(source: String) {
    private val dataDirPath = source

    private suspend fun zipContainer() {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val backupDir = File(dataDirPath)
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
                val zipFile = ZipFile("$dataDirPath/data.zip", "pass123".toCharArray())
                zipFile.addFile(zipContainer, zipParameters)
                zipContainer.delete()
            }
        }
    }

    suspend fun zipApk(apkFilesList: MutableList<File>, destination: String) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val zipParameters = ZipParameters()
                zipParameters.compressionMethod = CompressionMethod.STORE
                val zipFile = ZipFile("$destination/apk.zip")
                zipFile.addFiles(apkFilesList, zipParameters)
            }
        }
    }

}