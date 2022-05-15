package com.stefan.simplebackup.data.model

import android.os.Parcel
import android.os.Parcelable

@Suppress("ArrayInDataClass")
data class NotificationData(
    var name: String = "",
    var text: String = "",
    var image: ByteArray = byteArrayOf(),
    var progress: Int = 0
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

    companion object CREATOR : Parcelable.Creator<NotificationData> {
        override fun createFromParcel(parcel: Parcel): NotificationData {
            return NotificationData(parcel)
        }

        override fun newArray(size: Int): Array<NotificationData?> {
            return arrayOfNulls(size)
        }
    }
}
