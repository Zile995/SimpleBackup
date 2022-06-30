package com.stefan.simplebackup.data.model

import android.os.Parcel
import android.os.Parcelable

data class NotificationData(
    val name: String = "",
    val text: String = "",
    val image: ByteArray = byteArrayOf(),
    val progress: Int = 0
) : Parcelable {

    constructor(parcel: Parcel) : this(
        name = parcel.readString() ?: "",
        text = parcel.readString() ?: "",
        image = ByteArray(parcel.readInt()).also { byteArrayBitmap ->
            parcel.readByteArray(byteArrayBitmap) },
        progress = parcel.readInt()
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(text)
        dest.writeInt(image.size)
        dest.writeByteArray(image)
        dest.writeInt(progress)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationData

        if (name != other.name) return false
        if (text != other.text) return false
        if (!image.contentEquals(other.image)) return false
        if (progress != other.progress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + image.contentHashCode()
        result = 31 * result + progress
        return result
    }

    companion object CREATOR : Parcelable.Creator<NotificationData> {
        override fun createFromParcel(parcel: Parcel): NotificationData {
            return NotificationData(parcel)
        }

        override fun newArray(size: Int): Array<NotificationData?> {
            return arrayOfNulls(size)
        }
    }
}
