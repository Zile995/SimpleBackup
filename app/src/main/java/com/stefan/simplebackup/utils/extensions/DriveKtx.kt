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

suspend fun Drive.getFileWithParentIds(fileId: String): File =
    files()
        .get(fileId)
        .setFields("parents")
        .executeOnBackground()

suspend fun Drive.createFile(fileMetadata: File, fileContent: FileContent? = null): String {
    val file = files()
        .run {
            if (fileContent == null)
                create(fileMetadata)
            else
                create(fileMetadata, fileContent)
        }
        .setFields("id, parents")
        .executeOnBackground()
    return file.id
}

suspend fun Drive.fetchOrCreateMainFolder(folderName: String): String {
    val folderList = getFileList(mimeType = DRIVE_FOLDER.mimeType)
    Log.d("DriveService", "Folder list = ${folderList.files.map { it.name }}")
    return if (folderList.files.isEmpty()) {
        createFolder(folderName)
    } else {
        // Main folder is always latest in folder hierarchy
        folderList.getRootFolder().id
    }
}

suspend fun Drive.createFolder(folderName: String, parentId: String? = null): String {
    val folderList = getFileList()
    val folder = folderList.findFirstFileByName(folderName)

    if (folder != null)
        return folder.id

    val fileMetadata = File().apply {
        name = folderName
        mimeType = DRIVE_FOLDER.mimeType
        parents = parentId?.let { listOf(it) }
    }
    return createFile(fileMetadata)
}

suspend fun Drive.deleteFile(fileName: String) {
    val folderList = getFileList()
    val file = folderList.findFirstFileByName(fileName)
    file?.let {
        files()
            .delete(it.id)
            .executeOnBackground()
    }
}

suspend fun Drive.moveFile(fileId: String, folderId: String): String {
    val previousParentIds = buildString {
        getFileWithParentIds(fileId).parents.forEach { parentId ->
            append(parentId)
            append(',')
        }
    }
    Log.d("DriveService", "Previous parent list $previousParentIds")
    val file = files().update(fileId, null)
        .setAddParents(folderId)
        .setRemoveParents(previousParentIds)
        .setFields("id, parents")
        .executeOnBackground()
    return file.id
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
    return createFile(fileMetadata, fileContent)
}

suspend fun Drive.getFileList(mimeType: String = DRIVE_FOLDER.mimeType): FileList =
    files().list()
        .setSpaces("drive")
        .setQ("mimeType = '${mimeType}'")
        .executeOnBackground()

// FileList extensions
fun FileList.getRootFolder(): File = files.last()

fun FileList.findFirstFileByName(fileName: String): File? = files.find { it.name == fileName }
