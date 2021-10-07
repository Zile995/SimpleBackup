package com.stefan.simplebackup.drive

import com.google.api.services.drive.Drive
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DriveServiceHelper {
    private val mExecutor: Executor = Executors.newSingleThreadExecutor()
    private var mDriveService: Drive? = null

    fun DriveServiceHelper(driveService: Drive) {
        mDriveService = driveService
    }

}