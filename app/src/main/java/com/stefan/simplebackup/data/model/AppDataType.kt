package com.stefan.simplebackup.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class AppDataType : Parcelable {
    USER, LOCAL, CLOUD
}