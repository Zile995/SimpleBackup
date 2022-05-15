package com.stefan.simplebackup.data.model

import androidx.room.ColumnInfo

@Suppress("ArrayInDataClass")
data class ProgressData(
    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "bitmap")
    val bitmap: ByteArray,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "version_name")
    val versionName: String,

    @ColumnInfo(name = "is_split")
    val isSplit: Boolean,

    @ColumnInfo(name = "is_user_app")
    val isUserApp: Boolean,

    @ColumnInfo(name = "favorite")
    val favorite: Boolean
)