package com.stefan.simplebackup.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.FloatRange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAIN_PREFERENCE = "main_preference"
private const val SEQUENCE_NUMBER = "sequence_number"
private const val DATABASE_CREATED = "database_created"
private const val EXCLUDE_APPS_CACHE = "exclude_apps_cache"
private const val DOUBLE_PRESS_TO_EXIT = "double_press_to_exit"
private const val CHECKED_ROOT_GRANTED = "checked_root_granted"
private const val CHECKED_DEVICE_ROOTED = "checked_device_rooted"
private const val ZIP_COMPRESSION_LEVEL = "zip_compression_level"

object PreferenceHelper {

    private lateinit var sharedPreferences: SharedPreferences
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    val savedSequenceNumber: Int
        get() = sharedPreferences.getPreference(SEQUENCE_NUMBER, 0)
    val isDatabaseCreated: Boolean
        get() = sharedPreferences.getPreference(DATABASE_CREATED, false)
    val shouldExcludeAppsCache: Boolean
        get() = sharedPreferences.getPreference(EXCLUDE_APPS_CACHE, true)
    val savedZipCompressionLevel: Float
        get() = sharedPreferences.getPreference(ZIP_COMPRESSION_LEVEL, 1f)

    val shouldDoublePressToExit
        get() = sharedPreferences.getPreference(DOUBLE_PRESS_TO_EXIT, true)
    val hasCheckedRootGranted: Boolean
        get() = sharedPreferences.getPreference(CHECKED_ROOT_GRANTED, false)
    val hasCheckedDeviceRooted: Boolean
        get() = sharedPreferences.getPreference(CHECKED_DEVICE_ROOTED, false)

    fun Context.initPreferences() {
        sharedPreferences =
            applicationContext.getSharedPreferences(MAIN_PREFERENCE, Context.MODE_PRIVATE)
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

    suspend fun setDoublePressToExit(shouldDoublePress: Boolean) {
        sharedPreferences.editPreference(DOUBLE_PRESS_TO_EXIT, shouldDoublePress)
    }

    suspend fun setExcludeAppsCache(shouldExclude: Boolean) {
        sharedPreferences.editPreference(EXCLUDE_APPS_CACHE, shouldExclude)
    }

    suspend fun saveZipCompressionLevel(@FloatRange(from = 0.0, to = 9.0) compressionLevel: Float) {
        sharedPreferences.editPreference(ZIP_COMPRESSION_LEVEL, compressionLevel)
    }

    suspend fun resetSequenceNumber() {
        sharedPreferences.editPreference(SEQUENCE_NUMBER, 0)
    }

    suspend fun updateSequenceNumber(newSequenceNumber: Int) {
        sharedPreferences.editPreference(SEQUENCE_NUMBER, newSequenceNumber)
    }

    suspend fun setDatabaseCreated(isCreated: Boolean) {
        sharedPreferences.editPreference(DATABASE_CREATED, isCreated)
    }

    suspend fun setCheckedRootGranted(isChecked: Boolean) {
        sharedPreferences.editPreference(CHECKED_ROOT_GRANTED, isChecked)
    }

    suspend fun setCheckedDeviceRooted(isChecked: Boolean) {
        sharedPreferences.editPreference(CHECKED_DEVICE_ROOTED, isChecked)
    }

    fun registerPreferenceListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterPreferenceListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}