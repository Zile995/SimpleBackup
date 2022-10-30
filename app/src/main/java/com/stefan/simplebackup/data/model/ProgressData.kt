package com.stefan.simplebackup.data.model

import android.os.Parcelable
import com.stefan.simplebackup.utils.work.WorkResult
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProgressData(
    val index: Int,
    val name: String,
    val image: ByteArray,
    val message: String,
    val progress: Int,
    val workResult: WorkResult? = null
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProgressData

        if (name != other.name) return false
        if (message != other.message) return false
        if (!image.contentEquals(other.image)) return false
        if (progress != other.progress) return false
        if (index != other.index) return false
        if (workResult != other.workResult) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + image.contentHashCode()
        result = 31 * result + progress
        result = 31 * result + index
        result = 31 * result + workResult.hashCode()
        return result
    }
}
