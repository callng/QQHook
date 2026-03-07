package moe.ore.txhook.app.model

import android.os.Parcel
import android.os.Parcelable
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY

data class CaptureAction(
    var type: Int = 0,
) : Parcelable {
    var what: Int = 0
    var from: Boolean = false
    val time: Long = System.currentTimeMillis()
    var buffer: ByteArray = EMPTY_BYTE_ARRAY
    var result: ByteArray = EMPTY_BYTE_ARRAY
    var source: Int = SourceApp.MQQ
    var key: ByteArray = EMPTY_BYTE_ARRAY

    constructor(parcel: Parcel) : this(parcel.readInt()) {
        what = parcel.readInt()
        from = parcel.readByte() != 0.toByte()
        buffer = parcel.createByteArray()!!
        result = parcel.createByteArray()!!
        source = parcel.readInt()
        key = parcel.createByteArray()!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(type)
        parcel.writeInt(what)
        parcel.writeByte(if (from) 1 else 0)
        parcel.writeByteArray(buffer)
        parcel.writeByteArray(result)
        parcel.writeInt(source)
        parcel.writeByteArray(key)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CaptureAction> {
        override fun createFromParcel(parcel: Parcel): CaptureAction = CaptureAction(parcel)
        override fun newArray(size: Int): Array<CaptureAction?> = arrayOfNulls(size)
    }
}

data class CapturePacket(
    var from: Boolean = false,
    var cmd: String = "wtlogin.login",
    var seq: Int = 0,
    var buffer: ByteArray = EMPTY_BYTE_ARRAY,
    var time: Long = 0L,
    var uin: Long = 0L,
    var msgCookie: ByteArray = EMPTY_BYTE_ARRAY,
    var type: String = "",
    var source: Int = SourceApp.MQQ,
    var merge: Boolean = false,
    var hash: Int = 0,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.createByteArray()!!,
        parcel.readLong(),
        parcel.readLong(),
        parcel.createByteArray()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (from) 1 else 0)
        parcel.writeString(cmd)
        parcel.writeInt(seq)
        parcel.writeByteArray(buffer)
        parcel.writeLong(time)
        parcel.writeLong(uin)
        parcel.writeByteArray(msgCookie)
        parcel.writeString(type)
        parcel.writeInt(source)
        parcel.writeByte(if (merge) 1 else 0)
        parcel.writeInt(hash)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CapturePacket> {
        override fun createFromParcel(parcel: Parcel): CapturePacket = CapturePacket(parcel)
        override fun newArray(size: Int): Array<CapturePacket?> = arrayOfNulls(size)

        fun create(buffer: ByteArray): CapturePacket {
            return CapturePacket(buffer = buffer)
        }
    }
}

object SourceApp {
    const val MQQ = 0
    const val TIM = 1
    const val QQLITE = 2
    const val QIDIAN = 3
}
