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
    private val sourceFile = source

    private suspend fun zipContainer() {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val backupDir = File(sourceFile)
                var containerFilePath = ""
                backupDir.listFiles()?.filter {
                    it.isFile && it.extension == "tar"
                }?.forEach {
                    containerFilePath = it.absolutePath
                }

                val zipParameters = ZipParameters()
                with(zipParameters) {
                    isEncryptFiles = true
                    compressionMethod = CompressionMethod.DEFLATE
                    compressionLevel = CompressionLevel.FASTEST
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }

                val zipContainer = File(containerFilePath)
                val zipFile = ZipFile("$sourceFile/data.zip", "pass123".toCharArray())
                zipFile.addFile(zipContainer, zipParameters)
                zipContainer.delete()
            }
        }
    }

    private suspend fun zipApk(apkFilesList: MutableList<File>, target: String) {
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val zipParameters = ZipParameters()
                zipParameters.compressionMethod = CompressionMethod.STORE
                val zipFile = ZipFile("$target/apk.zip")
                zipFile.addFiles(apkFilesList, zipParameters)
            }
        }
    }

}