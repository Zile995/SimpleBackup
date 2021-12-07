package com.stefan.simplebackup.data


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Klasa koja će sadržati sve podatke o aplikaciji
 */
@Entity (tableName = "app_table")
@Keep
@Serializable
data class Application(
    @Transient
    @PrimaryKey(autoGenerate = true)
    private var uid: Int = 0,

    @ColumnInfo(name = "name")
    private var name: String = "",

    @ColumnInfo(name = "bitmap")
    private var bitmap: ByteArray = byteArrayOf(),

    @ColumnInfo(name = "package_name")
    private var packageName: String = "",

    @ColumnInfo(name = "version_name")
    private var versionName: String = "",

    @ColumnInfo(name = "target_sdk")
    private var targetSdk: Int = 0,

    @ColumnInfo(name = "min_sdk")
    private var minSdk: Int = 0,

    @ColumnInfo(name = "data_dir")
    private var dataDir: String = "",

    @ColumnInfo(name = "apk_dir")
    private var apkDir: String = "",

    @ColumnInfo(name = "date")
    private var date: String = "",

    @ColumnInfo(name = "data_size")
    private var dataSize: String = "",

    @ColumnInfo(name = "apk_size")
    private var apkSize: Float = 0f,

    @ColumnInfo(name = "favorites")
    private var favorites: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this() {
        readFromParcel(parcel)
    }

    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    /**
     * * U parceli može postojati i null String tako da readString() može čitati i String? tip
     * * Međutim imamo definisane String varijable u Application klasi
     * * Ukoliko je pročitan String null, upiši prazan String "" u konstruktor Application klase
     */
    private fun readFromParcel(parcel: Parcel) {
        uid = parcel.readInt()
        name = parcel.readString() ?: ""
        bitmap = ByteArray(parcel.readInt())
        parcel.readByteArray(bitmap)
        packageName = parcel.readString() ?: ""
        versionName = parcel.readString() ?: ""
        targetSdk = parcel.readInt()
        minSdk = parcel.readInt()
        dataDir = parcel.readString() ?: ""
        apkDir = parcel.readString() ?: ""
        date = parcel.readString() ?: ""
        dataSize = parcel.readString() ?: ""
        apkSize = parcel.readFloat()
        favorites = parcel.readInt()
    }

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
        dest.writeString(date)
        dest.writeString(dataSize)
        dest.writeFloat(apkSize)
        dest.writeInt(favorites)
    }

    fun getUid() = this.uid

    fun getName() = this.name

    /**
     * Specifični getter nazivi potrebnih funkcija kako bi room prepoznao polja
     */
    fun getBitmap() = this.bitmap

    fun getBitmapFromArray(): Bitmap? {
        return if (this.bitmap.isNotEmpty()) {
            BitmapFactory.decodeByteArray(this.bitmap, 0, this.bitmap.size)
        } else {
            null
        }
    }

    fun getPackageName() = this.packageName

    fun getVersionName() = this.versionName

    fun getTargetSdk() = this.targetSdk

    fun getMinSdk() = this.minSdk

    fun getDataDir() = this.dataDir

    fun getDataSize() = this.dataSize

    fun getApkSize() = this.apkSize

    fun getDate() = this.date

    fun getApkDir() = this.apkDir

    fun getFavorites() = this.favorites

    fun setUid(uid: Int) {
        this.uid = uid
    }

    fun setName(name: String) {
        this.name = name
    }

    fun setBitmap(bitmap: ByteArray) {
        this.bitmap = bitmap
    }

    fun setPackageName(packageName: String) {
        this.packageName = packageName
    }

    fun setVersionName(versionName: String) {
        this.versionName = versionName
    }

    fun setTargetSdk(targetSdk: Int) {
        this.targetSdk = targetSdk
    }

    fun setMinSdk(minSdk: Int) {
        this.minSdk = minSdk
    }

    fun setDataDir(newDir: String) {
        this.dataDir = newDir
    }

    fun setApkDir(apkDir: String) {
        this.apkDir = apkDir
    }

    fun setDate(newDate: String) {
        this.date = newDate
    }

    fun setDataSize(newSize: String) {
        this.dataSize = newSize
    }

    fun setApkSize(apkSize: Float) {
        this.apkSize = apkSize
    }

    fun setFavorites(favorites: Int) {
        this.favorites = favorites
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Application

        if (!bitmap.contentEquals(other.bitmap)) return false

        return true
    }

    override fun hashCode(): Int {
        return bitmap.contentHashCode()
    }

    companion object CREATOR : Parcelable.Creator<Application> {
        override fun createFromParcel(parcel: Parcel): Application {
            return Application(parcel)
        }

        override fun newArray(size: Int): Array<Application?> {
            return arrayOfNulls(size)
        }
    }
}