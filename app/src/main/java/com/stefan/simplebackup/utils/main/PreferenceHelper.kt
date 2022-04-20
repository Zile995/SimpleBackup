package com.stefan.simplebackup.utils.main

import android.content.Context
import android.content.SharedPreferences
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

    fun SharedPreferences.getPackageName() = this.getString(SHARED_PACKAGE_NAME, null)

    private suspend fun SharedPreferences.editBooleanPreference(preferenceName: String, value: Boolean) {
        withContext(ioDispatcher) {
            this.apply {
                edit()
                    .putBoolean(preferenceName, value)
                    .apply()
            }
        }
    }

    fun resetSequenceNumber() {
        sharedPreferences.apply {
            edit()
                .putInt(SEQUENCE_NUMBER, 0)
                .apply()
        }
    }

    fun updateSequenceNumber(newSequenceNumber: Int) {
        sharedPreferences.apply {
            edit()
                .putInt(SEQUENCE_NUMBER, newSequenceNumber)
                .apply()
        }
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

    fun savePackageName(packageName: String) {
        sharedPreferences.apply {
            edit()
                .putString(SHARED_PACKAGE_NAME, packageName)
                .apply()
        }
    }

    suspend fun setDatabaseCreated(isCreated: Boolean) {
        sharedPreferences.editBooleanPreference(DATABASE_CREATED, isCreated)
    }

    suspend fun setRootGranted(isGranted: Boolean) {
        sharedPreferences.editBooleanPreference(ROOT_GRANTED, isGranted)
    }

    suspend fun setRootChecked(isChecked: Boolean) {
        sharedPreferences.editBooleanPreference(ROOT_CHECKED, isChecked)
    }
}