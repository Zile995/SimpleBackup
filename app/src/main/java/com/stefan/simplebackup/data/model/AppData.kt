package com.stefan.simplebackup.data.model


import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stefan.simplebackup.utils.extensions.launchActivity
import com.stefan.simplebackup.utils.extensions.saveByteArray
import com.stefan.simplebackup.utils.work.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

const val APP_TABLE_NAME = "app_table"
const val PARCELABLE_EXTRA = "APPLICATION_DATA"
const val APP_DATA_TYPE_EXTRA = "APP_DATA_TYPE"

/**
 * - Main model data class
 */
@Keep
@Serializable
@Entity(
    tableName = APP_TABLE_NAME,
    indices = [Index(value = ["package_name", "is_local"], unique = true)]
)
data class AppData(
    @Transient @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "uid") val uid: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "bitmap") var bitmap: ByteArray,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "version_name") val versionName: String,
    @ColumnInfo(name = "date") var date: Long,
    @ColumnInfo(name = "min_sdk") val minSdk: Int,
    @ColumnInfo(name = "target_sdk") val targetSdk: Int,
    @ColumnInfo(name = "data_dir") val dataDir: String,
    @Transient @ColumnInfo(name = "apk_dir") val apkDir: String = "",
    @ColumnInfo(name = "apk_size") val apkSize: Float,
    @ColumnInfo(name = "data_size") var dataSize: Long = 0L,
    @ColumnInfo(name = "is_split") val isSplit: Boolean,
    @ColumnInfo(name = "cache_size") var cacheSize: Long = 0L,
    @ColumnInfo(name = "is_favorite") var isFavorite: Boolean = false,
    @ColumnInfo(name = "is_user_app") var isUserApp: Boolean = true,
    @ColumnInfo(name = "is_local") var isLocal: Boolean = false
) : Parcelable {

    var isSelected = false

    constructor(parcel: Parcel) : this(
        uid = parcel.readInt(),
        name = parcel.readString() ?: "",
        bitmap = ByteArray(parcel.readInt()).also { byteArrayBitmap ->
            parcel.readByteArray(byteArrayBitmap)
        },
        packageName = parcel.readString() ?: "",
        versionName = parcel.readString() ?: "",
        date = parcel.readLong(),
        targetSdk = parcel.readInt(),
        minSdk = parcel.readInt(),
        dataDir = parcel.readString() ?: "",
        apkDir = parcel.readString() ?: "",
        apkSize = parcel.readFloat(),
        isSplit = parcel.readBooleanValue() ?: false,
        dataSize = parcel.readLong(),
        cacheSize = parcel.readLong(),
        isFavorite = parcel.readBooleanValue() ?: false,
        isUserApp = parcel.readBooleanValue() ?: false,
        isLocal = parcel.readBooleanValue() ?: false
    )

    fun getDateText() = convertDateToString()

    fun setCurrentDate() {
        date = System.currentTimeMillis()
    }

    @Throws(IOException::class)
    suspend fun serialize(destinationPath: String) {
        isLocal = true
        JsonUtil.serializeApp(app = this, destinationPath = destinationPath)
    }

    private fun convertDateToString(): String {
        val locale = Locale.getDefault()
        val dateFormat = SimpleDateFormat(
            "dd MMM yyyy HH:mm", locale
        )
        return dateFormat.format(this.date)
    }

    suspend inline fun <reified T : AppCompatActivity> passToActivity(
        context: Context?
    ) = context?.run { launchActivity<T>(PARCELABLE_EXTRA to withCheckedBitmap(context)) }

    suspend fun withCheckedBitmap(context: Context): AppData = run {
        if (bitmap.size > 200_000) {
            bitmap.saveByteArray(name, context)
            copy(bitmap = byteArrayOf())
        } else {
            this
        }
    }

    suspend inline fun setBitmapFromContextDir(
        context: Context,
        crossinline onFailure: suspend (Context) -> ByteArray
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (bitmap.isNotEmpty())
                    return@withContext
                context.openFileInput(name).use { stream ->
                    bitmap = stream.readBytes()
                }
                context.deleteFile(name)
                if (bitmap.isEmpty()) onFailure(context)
            } catch (e: IOException) {
                bitmap = onFailure(context)
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
        dest.writeLong(date)
        dest.writeInt(targetSdk)
        dest.writeInt(minSdk)
        dest.writeString(dataDir)
        dest.writeString(apkDir)
        dest.writeFloat(apkSize)
        dest.writeBooleanValue(isSplit)
        dest.writeLong(dataSize)
        dest.writeLong(cacheSize)
        dest.writeBooleanValue(isFavorite)
        dest.writeBooleanValue(isUserApp)
        dest.writeBooleanValue(isLocal)
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
        if (date != other.date) return false
        if (targetSdk != other.targetSdk) return false
        if (minSdk != other.minSdk) return false
        if (dataDir != other.dataDir) return false
        if (apkDir != other.apkDir) return false
        if (apkSize != other.apkSize) return false
        if (isSplit != other.isSplit) return false
        if (dataSize != other.dataSize) return false
        if (cacheSize != other.cacheSize) return false
        if (isUserApp != other.isUserApp) return false
        if (isFavorite != other.isFavorite) return false
        if (isLocal != other.isLocal) return false

        return true
    }

    override fun describeContents(): Int = 0

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bitmap.contentHashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + versionName.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + targetSdk
        result = 31 * result + minSdk
        result = 31 * result + dataDir.hashCode()
        result = 31 * result + apkDir.hashCode()
        result = 31 * result + apkSize.hashCode()
        result = 31 * result + isSplit.hashCode()
        result = 31 * result + dataSize.hashCode()
        result = 31 * result + cacheSize.hashCode()
        result = 31 * result + isUserApp.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + isLocal.hashCode()
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

@Parcelize
enum class AppDataType : Parcelable {
    USER, LOCAL, CLOUD
}