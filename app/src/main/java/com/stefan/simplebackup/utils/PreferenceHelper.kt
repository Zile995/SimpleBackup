package com.stefan.simplebackup.utils

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.FloatRange
import com.stefan.simplebackup.data.model.AppDataType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PROGRESS_TYPE = "app_data_type"
private const val MAIN_PREFERENCE = "main_preference"
private const val SEQUENCE_NUMBER = "sequence_number"
private const val DATABASE_CREATED = "database_created"
private const val NUM_OF_WORK_ITEMS = "num_of_work_items"
private const val EXCLUDE_APPS_CACHE = "exclude_apps_cache"
private const val DOUBLE_PRESS_TO_EXIT = "double_press_to_exit"
private const val CHECKED_ROOT_GRANTED = "checked_root_granted"
private const val CHECKED_DEVICE_ROOTED = "checked_device_rooted"
private const val ZIP_COMPRESSION_LEVEL = "zip_compression_level"

object PreferenceHelper {

    private lateinit var sharedPreferences: SharedPreferences
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // PackageListener preferences
    val savedSequenceNumber: Int
        get() = sharedPreferences.getPreference(SEQUENCE_NUMBER, 0)
    val isDatabaseCreated: Boolean
        get() = sharedPreferences.getPreference(DATABASE_CREATED, false)

    // Settings preferences
    val shouldExcludeAppsCache: Boolean
        get() = sharedPreferences.getPreference(EXCLUDE_APPS_CACHE, true)
    val savedZipCompressionLevel: Float
        get() = sharedPreferences.getPreference(ZIP_COMPRESSION_LEVEL, 1f)
    val shouldDoublePressToExit
        get() = sharedPreferences.getPreference(DOUBLE_PRESS_TO_EXIT, true)

    // RootChecker preferences
    val hasCheckedRootGranted: Boolean
        get() = sharedPreferences.getPreference(CHECKED_ROOT_GRANTED, false)
    val hasCheckedDeviceRooted: Boolean
        get() = sharedPreferences.getPreference(CHECKED_DEVICE_ROOTED, false)

    // Progress preferences
    val progressType: Int
        get() = sharedPreferences.getPreference(PROGRESS_TYPE, 0)
    val numOfWorkItems: Int
        get() = sharedPreferences.getPreference(NUM_OF_WORK_ITEMS, 1)

    // Init main preference
    fun Application.initPreferences() {
        sharedPreferences = getSharedPreferences(MAIN_PREFERENCE, Context.MODE_PRIVATE)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> SharedPreferences.getPreference(
        preferenceName: String, defaultValue: T
    ): T = when (defaultValue) {
        is Int -> getInt(preferenceName, defaultValue)
        is Float -> getFloat(preferenceName, defaultValue)
        is String -> getString(preferenceName, defaultValue)
        is Boolean -> getBoolean(preferenceName, defaultValue)
        else -> throw IllegalArgumentException("Unsupported type")
    } as T

    private suspend fun <T> SharedPreferences.editPreference(preferenceName: String, value: T) {
        withContext(ioDispatcher) {
            edit().apply {
                when (value) {
                    is Int -> putInt(preferenceName, value)
                    is Float -> putFloat(preferenceName, value)
                    is Boolean -> putBoolean(preferenceName, value)
                    is String, is String? -> putString(preferenceName, value as String?)
                    else -> throw IllegalArgumentException("Unsupported type")
                }
                apply()
            }
        }
    }

    private suspend fun SharedPreferences.removePreference(preferenceName: String) {
        withContext(ioDispatcher) {
            edit().apply {
                remove(preferenceName)
                apply()
            }
        }
    }

    fun hasSavedProgressData() =
        sharedPreferences.run { contains(PROGRESS_TYPE) || contains(NUM_OF_WORK_ITEMS) }

    suspend fun removeProgressData() = sharedPreferences.run {
        removePreference(PROGRESS_TYPE)
        removePreference(NUM_OF_WORK_ITEMS)
    }

    suspend fun saveNumOfWorkItems(numOfWorkItems: Int) =
        sharedPreferences.editPreference(NUM_OF_WORK_ITEMS, numOfWorkItems)

    suspend fun setDoublePressToExit(shouldDoublePress: Boolean) =
        sharedPreferences.editPreference(DOUBLE_PRESS_TO_EXIT, shouldDoublePress)

    suspend fun setExcludeAppsCache(shouldExclude: Boolean) =
        sharedPreferences.editPreference(EXCLUDE_APPS_CACHE, shouldExclude)


    suspend fun saveZipCompressionLevel(@FloatRange(from = 0.0, to = 9.0) compressionLevel: Float) =
        sharedPreferences.editPreference(ZIP_COMPRESSION_LEVEL, compressionLevel)

    suspend fun resetSequenceNumber() = sharedPreferences.editPreference(SEQUENCE_NUMBER, 0)

    suspend fun updateSequenceNumber(newSequenceNumber: Int) =
        sharedPreferences.editPreference(SEQUENCE_NUMBER, newSequenceNumber)

    suspend fun setDatabaseCreated(isCreated: Boolean) =
        sharedPreferences.editPreference(DATABASE_CREATED, isCreated)

    suspend fun setCheckedRootGranted(isChecked: Boolean) =
        sharedPreferences.editPreference(CHECKED_ROOT_GRANTED, isChecked)

    suspend fun setCheckedDeviceRooted(isChecked: Boolean) =
        sharedPreferences.editPreference(CHECKED_DEVICE_ROOTED, isChecked)

    suspend fun saveProgressType(progressType: AppDataType) =
        sharedPreferences.editPreference(PROGRESS_TYPE, progressType.ordinal)
}