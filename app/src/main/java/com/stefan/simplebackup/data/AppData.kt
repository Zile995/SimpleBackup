package com.stefan.simplebackup.data


import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Klasa koja će sadržati sve podatke o aplikaciji
 */
@Suppress("unused")
@Entity(tableName = "app_table", indices = [Index(value = ["package_name"], unique = true)])
@Keep
@Serializable
data class AppData(
    @Transient
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "bitmap")
    var bitmap: ByteArray,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "version_name")
    val versionName: String,

    @Transient
    @ColumnInfo(name = "target_sdk")
    val targetSdk: Int = 0,

    @Transient
    @ColumnInfo(name = "min_sdk")
    val minSdk: Int = 0,

    @ColumnInfo(name = "data_dir")
    val dataDir: String,

    @ColumnInfo(name = "apk_dir")
    val apkDir: String,

    @ColumnInfo(name = "apk_size")
    val apkSize: Float,

    @ColumnInfo(name = "split")
    val split: Boolean,

    @ColumnInfo(name = "data_size")
    var dataSize: Long,

    @ColumnInfo(name = "cache_size")
    var cacheSize: Long,

    @ColumnInfo(name = "favorite")
    var favorite: Boolean
) : Parcelable {

    @ColumnInfo(name = "date")
    var date: String = ""

    /**
     * * U parceli može postojati i null String tako da readString() može čitati i String? tip
     * * Međutim imamo definisane String varijable u AppData klasi
     * * Ukoliko je pročitan String null, upiši prazan String "" u konstruktor AppData klase
     */
    constructor(parcel: Parcel) : this(
        uid = parcel.readInt(),
        name = parcel.readString() ?: "",
        bitmap = ByteArray(parcel.readInt()).also { byteArrayBitmap ->
            parcel.readByteArray(byteArrayBitmap) },
        packageName = parcel.readString() ?: "",
        versionName = parcel.readString() ?: "",
        targetSdk = parcel.readInt(),
        minSdk = parcel.readInt(),
        dataDir = parcel.readString() ?: "",
        apkDir = parcel.readString() ?: "",
        apkSize = parcel.readFloat(),
        split = parcel.readBooleanValue() ?: false,
        dataSize = parcel.readLong(),
        cacheSize = parcel.readLong(),
        favorite = parcel.readBooleanValue() ?: false
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(uid)
        dest.writeString(name)
        dest.writeInt(bitmap.size)
        dest.writeByteArray(bitmap)
        dest.writeString(packageName)
        dest.writeString(versionName)
        dest.writeInt(targetSdk)
        dest.writeInt(minSdk)
        dest.writeString(dataDir)
        dest.writeString(apkDir)
        dest.writeFloat(apkSize)
        dest.writeBooleanValue(split)
        dest.writeLong(dataSize)
        dest.writeLong(cacheSize)
        dest.writeBooleanValue(favorite)
        dest.writeString(date)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppData

        if (!bitmap.contentEquals(other.bitmap)) return false

        return true
    }

    override fun describeContents(): Int = 0

    override fun hashCode(): Int {
        return bitmap.contentHashCode()
    }

    companion object CREATOR : Parcelable.Creator<AppData> {
        override fun createFromParcel(parcel: Parcel): AppData {
            return AppData(parcel)
        }

        override fun newArray(size: Int): Array<AppData?> {
            return arrayOfNulls(size)
        }

        fun Parcel.writeBooleanValue(flag: Boolean?) {
            when (flag) {
                true -> writeInt(1)
                false -> writeInt(0)
                else -> writeInt(-1)
            }
        }

        fun Parcel.readBooleanValue(): Boolean? {
            return when (readInt()) {
                1 -> true
                0 -> false
                else -> null
            }
        }
    }
}