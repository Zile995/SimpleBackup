package com.stefan.simplebackup.utils

import android.content.Context
import android.content.SharedPreferences
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.withContext

private const val SEQUENCE_NUMBER = "sequence_number"
private const val MAIN_PREFERENCE = "main_preference"
private const val DATABASE_CREATED = "database_created"
private const val CHECKED_ROOT_GRANTED = "checked_root_granted"
private const val CHECKED_DEVICE_ROOTED = "checked_device_rooted"

object PreferenceHelper {
    private lateinit var sharedPreferences: SharedPreferences

    val getSequenceNumber: Int get() = sharedPreferences.getInt(SEQUENCE_NUMBER, 0)
    val isDatabaseCreated: Boolean get() = sharedPreferences.getBoolean(DATABASE_CREATED, false)
    val hasCheckedRootGranted: Boolean
        get() = sharedPreferences.getBoolean(
            CHECKED_ROOT_GRANTED,
            false
        )
    val hasCheckedDeviceRooted: Boolean
        get() = sharedPreferences.getBoolean(
            CHECKED_DEVICE_ROOTED,
            false
        )

    fun Context.initPreferences() {
        sharedPreferences = getSharedPreferences(MAIN_PREFERENCE, Context.MODE_PRIVATE)
    }

    private suspend fun <T> SharedPreferences.editPreference(preferenceName: String, value: T) {
        withContext(ioDispatcher) {
            edit().apply {
                when (value) {
                    is Int -> {
                        putInt(preferenceName, value)
                    }
                    is Float -> {
                        putFloat(preferenceName, value)
                    }
                    is Boolean -> {
                        putBoolean(preferenceName, value)
                    }
                    is String, is String? -> {
                        putString(preferenceName, value as String?)
                    }
                    else -> {
                        throw IllegalArgumentException("Unsupported type")
                    }
                }
                apply()
            }
        }
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
        listener: SharedPreferences
        .OnSharedPreferenceChangeListener
    ) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterPreferenceListener(
        listener: SharedPreferences
        .OnSharedPreferenceChangeListener
    ) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}