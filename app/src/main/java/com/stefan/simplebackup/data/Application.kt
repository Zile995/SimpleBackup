package com.stefan.simplebackup.data


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Klasa koja će sadržati sve podatke o aplikaciji
 */

@Keep
@Serializable
class Application(
    private var name: String = "",
    private var bitmap: ByteArray = byteArrayOf(),
    private var packageName: String = "",
    private var versionName: String = "",
    private var targetSdk: Int = 0,
    private var minSdk: Int = 0,
    private var dataDir: String = "",
    private var apkDir: String = "",
    private var date: String = "",
    private var dataSize: String = "",
    private var apkSize: Float = 0f
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
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
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
    }

    fun getName() = this.name

    fun getBitmap(): Bitmap? {
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

    fun setBitmap(bitmap: ByteArray) {
        this.bitmap = bitmap
    }

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