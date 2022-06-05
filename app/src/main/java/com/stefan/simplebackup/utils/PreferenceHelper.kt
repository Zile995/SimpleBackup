package com.stefan.simplebackup.utils

import android.content.Context
import android.content.SharedPreferences
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import kotlinx.coroutines.withContext

private const val MAIN_PREFERENCE = "main_preference"
private const val SHARED_PACKAGE_NAME = "package_name"
private const val SEQUENCE_NUMBER = "sequence_number"
private const val ROOT_GRANTED = "root_granted"
private const val ROOT_CHECKED = "root_checked"
private const val DATABASE_CREATED = "database_created"

object PreferenceHelper {
    private lateinit var sharedPreferences: SharedPreferences

    val packageName: String? get() = sharedPreferences.getString(SHARED_PACKAGE_NAME, null)
    val isRootGranted: Boolean get() = sharedPreferences.getBoolean(ROOT_GRANTED, false)
    val isRootChecked: Boolean get() = sharedPreferences.getBoolean(ROOT_CHECKED, false)
    val getSequenceNumber: Int get() = sharedPreferences.getInt(SEQUENCE_NUMBER, 0)
    val isDatabaseCreated: Boolean get() = sharedPreferences.getBoolean(DATABASE_CREATED, false)

    fun Context.initPreferences() {
        sharedPreferences = getSharedPreferences(MAIN_PREFERENCE, Context.MODE_PRIVATE)
    }

    private suspend fun <T> SharedPreferences.editPreference(preferenceName: String, value: T) {
        withContext(ioDispatcher) {
            edit().apply {
                when (value) {
                    is Boolean -> {
                        putBoolean(preferenceName, value)
                    }
                    is String, is String? -> {
                        putString(preferenceName, value as String?)
                    }
                    is Int -> {
                        putInt(preferenceName, value)
                    }
                    is Float -> {
                        putFloat(preferenceName, value)
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

    suspend fun clearPackageName() {
        sharedPreferences.editPreference(SHARED_PACKAGE_NAME, null)
    }

    suspend fun updateSequenceNumber(newSequenceNumber: Int) {
        sharedPreferences.editPreference(SEQUENCE_NUMBER, newSequenceNumber)
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

    suspend fun savePackageName(packageName: String?) {
        sharedPreferences.editPreference(SHARED_PACKAGE_NAME, packageName)
    }

    suspend fun setDatabaseCreated(isCreated: Boolean) {
        sharedPreferences.editPreference(DATABASE_CREATED, isCreated)
    }

    suspend fun setRootGranted(isGranted: Boolean) {
        sharedPreferences.editPreference(ROOT_GRANTED, isGranted)
    }

    suspend fun setRootChecked(isChecked: Boolean) {
        sharedPreferences.editPreference(ROOT_CHECKED, isChecked)
    }
}