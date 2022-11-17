package com.stefan.simplebackup.utils.extensions

import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.Consts
import org.apache.http.entity.ContentType

// Coroutine dispatcher
private val ioDispatcher = Dispatchers.IO

// App drive folder
private val DRIVE_FOLDER =
    ContentType.create("application/vnd.google-apps.folder", Consts.ISO_8859_1)

suspend fun <T> DriveRequest<T>.executeOnBackground(): T =
    withContext(ioDispatcher) {
        execute()
    }

suspend fun Drive.createFile(fileMetadata: File): String {
    val file = files()
        .create(fileMetadata)
        .setFields("id")
        .executeOnBackground()
    return file.id
}

suspend fun Drive.fetchOrCreateMainFolder(folderName: String): String {
    val folderList = getFileList(mimeType = DRIVE_FOLDER.mimeType)

    Log.d("DriveService", "Folder list = $folderList")
    return if (folderList.files.isEmpty()) {
        val fileMetadata = File().apply {
            name = folderName
            mimeType = DRIVE_FOLDER.mimeType
        }
        createFile(fileMetadata = fileMetadata)
    } else {
        // Main folder is always latest in folder hierarchy
        folderList.getRootFolder().id
    }
}

suspend fun Drive.createSubFolder(parentFolderId: String, subFolderName: String): String {
    val fileMetadata = File().apply {
        name = subFolderName
        mimeType = DRIVE_FOLDER.mimeType
        parents = listOf(parentFolderId)
    }

    Log.d("DriveService", "Name = ${fileMetadata.name}")
    Log.d("DriveService", "MimeType =  ${fileMetadata.mimeType}")

    return files()
        .create(fileMetadata)
        .setFields("id, parents")
        .executeOnBackground()
        .id
}

suspend fun Drive.deleteFile(fileId: String) {
    files()
        .delete(fileId)
        .executeOnBackground()
}

suspend fun Drive.uploadFileToFolder(
    inputFile: java.io.File,
    mimeType: String,
    parentFolderId: String,
): String {
    val fileMetadata = File().apply {
        name = inputFile.name
        parents = listOf(parentFolderId)
    }
    val fileContent = FileContent(mimeType, inputFile)

    return files()
        .create(fileMetadata, fileContent)
        .setFields("id, parents")
        .executeOnBackground()
        .id
}

suspend fun Drive.getFileList(mimeType: String): FileList =
    files()
        .list()
        .setSpaces("drive")
        .setQ("mimeType = '${mimeType}'")
        .executeOnBackground()

fun FileList.getRootFolder(): File = files.last()
