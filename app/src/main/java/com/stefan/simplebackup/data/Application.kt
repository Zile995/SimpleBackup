package com.stefan.simplebackup.data


import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Data klasa koja će sadržati sve podatke o aplikaciji
 */

@Keep
@Serializable
data class Application(
    private val name: String,
    private val packageName: String,
    private val versionName: String,
    private var dataDir: String,
    private var apkDir: String,
    private var date: String,
    private var size: Long
) : Parcelable {

    /**
     * Pošto readString vraća nullable String? a imamo definisane String varijable u data klasi,
     * ukoliko je null, vrati prazan String ""
     */
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong()
    )

    override fun describeContents(): Int {
        TODO("Not yet implemented")
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(packageName)
        dest.writeString(versionName)
        dest.writeString(dataDir)
        dest.writeString(apkDir)
        dest.writeLong(size)
    }

    fun getName() = this.name

    fun getPackageName() = this.packageName

    fun getVersionName() = this.versionName

    fun getDataDir() = this.dataDir

    fun getSize() = this.size

    fun getDate() = this.date

    fun getApkDir() = this.apkDir

    fun setDataDir(newDir: String) {
        this.dataDir = newDir
    }

    fun setSize(newSize: Long) {
        this.size = newSize
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