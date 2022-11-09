package com.stefan.simplebackup.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stefan.simplebackup.utils.work.WorkResult
import kotlinx.parcelize.Parcelize

const val PROGRESS_TABLE_NAME = "progress_table"

@Parcelize
@Entity(tableName = PROGRESS_TABLE_NAME, indices = [Index(value = ["package_name"], unique = true)])
data class ProgressData(
    @ColumnInfo(name = "index")
    @PrimaryKey(autoGenerate = false)
    val index: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "image")
    val image: ByteArray,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "progress")
    val progress: Int,

    @ColumnInfo(name = "work_result")
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
