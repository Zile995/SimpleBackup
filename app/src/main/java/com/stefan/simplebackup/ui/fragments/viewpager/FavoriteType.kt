package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class FavoriteType : Parcelable {
    USER, LOCAL, CLOUD
}