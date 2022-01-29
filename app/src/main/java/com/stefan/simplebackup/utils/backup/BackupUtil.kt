package com.stefan.simplebackup.utils.backup

import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.utils.FileUtil
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

const val ROOT: String = "SimpleBackup/local"

class BackupUtil(private val app: AppData, private val internalStoragePath: String) {

    private val mainBackupDir: String = internalStoragePath.run {
        substring(0, indexOf("Android")) + ROOT
    }
    private val appBackupDir: String = mainBackupDir + "${app.getName()}_${app.getVersionName()}"

    private fun createMainDir() {
        with(FileUtil) {
            createDirectory(mainBackupDir)
            createFile("$mainBackupDir/.nomedia")
        }
    }

    fun getApkList(): MutableList<File> {
        val dir = File(app.getApkDir())
        val apkList = mutableListOf<File>()
        dir.walkTopDown().filter {
            it.extension == "apk"
        }.forEach {
            apkList.add(it)
        }
        return apkList
    }

    fun createAppBackupDir(): String {
        val dir = File(appBackupDir)
        var number = 1
        val newPath: String
        return if (dir.exists()) {
            while (File(appBackupDir.plus("_$number")).exists()) {
                number++
            }
            newPath = appBackupDir.plus("_$number")
            File(newPath).mkdirs()
            newPath
        } else {
            FileUtil.createDirectory(appBackupDir)
            appBackupDir
        }
    }

    suspend fun calculateDataSize(path: String): String {
        return withContext(Dispatchers.IO) {
            if (Shell.rootAccess()) {
                val resultList = arrayListOf<String>()
                var result = ""
                Shell.su("du -sch $path/").to(resultList).exec()
                resultList.forEach {
                    if (it.contains("total")) {
                        result = it.removeSuffix("\ttotal")
                    }
                }
                if (result == "16K")
                    result = "0K"

                result = StringBuilder(result)
                    .insert(result.length - 1, " ")
                    .append("B")
                    .toString()

                result
            } else
                "Can't read"
        }
    }


}