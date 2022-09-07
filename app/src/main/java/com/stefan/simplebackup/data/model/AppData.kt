package com.stefan.simplebackup.data.model


import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stefan.simplebackup.R
import com.stefan.simplebackup.utils.extensions.getResourceDrawable
import com.stefan.simplebackup.utils.extensions.ioDispatcher
import com.stefan.simplebackup.utils.extensions.passBundleToActivity
import com.stefan.simplebackup.utils.file.BitmapUtil.saveByteArray
import com.stefan.simplebackup.utils.file.BitmapUtil.toByteArray
import com.stefan.simplebackup.utils.file.FileUtil.getTempDirPath
import com.stefan.simplebackup.utils.file.JsonUtil
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

const val PARCELABLE_EXTRA = "APPLICATION_DATA"

/**
 * - Main model data class
 */
@Keep
@Entity(
    tableName = "app_table",
    indices = [Index(value = ["package_name", "is_local", "is_cloud"], unique = true)]
)
@Serializable
data class AppData(
    @Transient
    @ColumnInfo(name = "uid")
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

    @ColumnInfo(name = "target_sdk")
    val targetSdk: Int,

    @ColumnInfo(name = "min_sdk")
    val minSdk: Int,

    @ColumnInfo(name = "data_dir")
    val dataDir: String,

    @ColumnInfo(name = "apk_dir")
    val apkDir: String,

    @ColumnInfo(name = "apk_size")
    val apkSize: Float,

    @ColumnInfo(name = "is_split")
    val isSplit: Boolean,

    @ColumnInfo(name = "data_size")
    var dataSize: Long,

    @ColumnInfo(name = "cache_size")
    var cacheSize: Long,

    @ColumnInfo(name = "favorite")
    var favorite: Boolean,

    @ColumnInfo(name = "is_user_app")
    var isUserApp: Boolean,

    @ColumnInfo(name = "is_local")
    var isLocal: Boolean = false,

    @ColumnInfo(name = "is_cloud")
    var isCloud: Boolean = false
) : Parcelable {

    @ColumnInfo(name = "date")
    var date: String = ""

    @ColumnInfo(name = "should_backup_data")
    var shouldBackupData = false

    @ColumnInfo(name = "should_backup_cache")
    var shouldBackupCache = false

    var isSelected = false

    constructor(parcel: Parcel) : this(
        uid = parcel.readInt(),
        name = parcel.readString() ?: "",
        bitmap = ByteArray(parcel.readInt()).also { byteArrayBitmap ->
            parcel.readByteArray(byteArrayBitmap)
        },
        packageName = parcel.readString() ?: "",
        versionName = parcel.readString() ?: "",
        targetSdk = parcel.readInt(),
        minSdk = parcel.readInt(),
        dataDir = parcel.readString() ?: "",
        apkDir = parcel.readString() ?: "",
        apkSize = parcel.readFloat(),
        isSplit = parcel.readBooleanValue() ?: false,
        dataSize = parcel.readLong(),
        cacheSize = parcel.readLong(),
        favorite = parcel.readBooleanValue() ?: false,
        isUserApp = parcel.readBooleanValue() ?: false,
        isLocal = parcel.readBooleanValue() ?: false,
        isCloud = parcel.readBooleanValue() ?: false
    )

    suspend fun serializeApp() {
        isLocal = true
        JsonUtil.serializeApp(this, getTempDirPath(this))
    }

    fun setBackupTime() {
        val locale = Locale.getDefault()
        val time = SimpleDateFormat(
            "dd.MM.yy-HH:mm", locale
        )
        date = time.format(Date())
    }

    suspend inline fun <reified T : AppCompatActivity> passToActivity(
        context: Context?
    ) {
        context?.apply {
            passBundleToActivity<T>(
                PARCELABLE_EXTRA to withCheckedBitmap(context)
            )
        }
    }

    suspend fun withCheckedBitmap(context: Context): AppData = run {
        if (bitmap.size > 200_000) {
            Log.d("Bitmap", "${bitmap.size}")
            bitmap.saveByteArray(name, context)
            copy(bitmap = byteArrayOf())
        } else {
            this
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun setBitmap(context: Context) {
        withContext(ioDispatcher) {
            try {
                if (bitmap.isNotEmpty())
                    return@withContext
                context.openFileInput(name).use { stream ->
                    bitmap = stream.readBytes()
                }
                context.deleteFile(name)
            } catch (exception: IOException) {
                bitmap =
                    context.getResourceDrawable(R.drawable.ic_error)?.toByteArray()
                        ?: byteArrayOf()
            }
        }
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
        dest.writeFloat(apkSize)
        dest.writeBooleanValue(isSplit)
        dest.writeLong(dataSize)
        dest.writeLong(cacheSize)
        dest.writeBooleanValue(favorite)
        dest.writeBooleanValue(isUserApp)
        dest.writeBooleanValue(isLocal)
        dest.writeBooleanValue(isCloud)
    }

    /**
     * - Equals method is really important.
     * - For example: equals method is useful for DiffUtil used in ListAdapter.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppData

        if (name != other.name) return false
        if (!bitmap.contentEquals(other.bitmap)) return false
        if (packageName != other.packageName) return false
        if (versionName != other.versionName) return false
        if (targetSdk != other.targetSdk) return false
        if (minSdk != other.minSdk) return false
        if (dataDir != other.dataDir) return false
        if (apkDir != other.apkDir) return false
        if (apkSize != other.apkSize) return false
        if (isSplit != other.isSplit) return false
        if (dataSize != other.dataSize) return false
        if (cacheSize != other.cacheSize) return false
        if (isUserApp != other.isUserApp) return false
        if (favorite != other.favorite) return false
        if (date != other.date) return false
        if (isLocal != other.isLocal) return false
        if (isCloud != other.isCloud) return false

        return true
    }

    override fun describeContents(): Int = 0

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bitmap.contentHashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + versionName.hashCode()
        result = 31 * result + targetSdk
        result = 31 * result + minSdk
        result = 31 * result + dataDir.hashCode()
        result = 31 * result + apkDir.hashCode()
        result = 31 * result + apkSize.hashCode()
        result = 31 * result + isSplit.hashCode()
        result = 31 * result + dataSize.hashCode()
        result = 31 * result + cacheSize.hashCode()
        result = 31 * result + isUserApp.hashCode()
        result = 31 * result + favorite.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + isLocal.hashCode()
        result = 31 * result + isCloud.hashCode()
        result = 31 * result + shouldBackupData.hashCode()
        result = 31 * result + shouldBackupCache.hashCode()
        return result
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