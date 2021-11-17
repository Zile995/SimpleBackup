package com.stefan.simplebackup.data


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Data klasa koja će sadržati sve podatke o aplikaciji
 */

@Keep
@Serializable
data class Application(
    private val name: String,
    @Transient private var bitmap: Bitmap? = null,
    private val packageName: String,
    private val versionName: String,
    private val targetSdk: Int,
    private val minSdk: Int,
    private var dataDir: String,
    private var apkDir: String,
    private var date: String,
    private var dataSize: String,
    private var apkSize: Float
) : Parcelable {

    /**
     * Pošto readString vraća nullable String? a imamo definisane String varijable u data klasi,
     * ukoliko je null, vrati prazan String ""
     */
    @SuppressLint("ParcelClassLoader")
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readParcelable<Bitmap>(null),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readFloat()
    )

    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeParcelable(bitmap, flags)
        dest.writeString(packageName)
        dest.writeString(versionName)
        dest.writeInt(targetSdk)
        dest.writeInt(minSdk)
        dest.writeString(dataDir)
        dest.writeString(apkDir)
        dest.writeString(date)
        dest.writeString(dataSize)
        dest.writeFloat(apkSize)
    }

    fun getName() = this.name

    fun getBitmap() = this.bitmap

    fun getPackageName() = this.packageName

    fun getVersionName() = this.versionName

    fun getTargetSdk() = this.targetSdk

    fun getMinSdk() = this.minSdk

    fun getDataDir() = this.dataDir

    fun getDataSize() = this.dataSize

    fun getApkSize() = this.apkSize

    fun getDate() = this.date

    fun getApkDir() = this.apkDir

    fun setDataDir(newDir: String) {
        this.dataDir = newDir
    }

    fun setDataSize(newSize: String) {
        this.dataSize = newSize
    }

    fun setDate(newDate: String) {
        this.date = newDate
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