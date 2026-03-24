package moe.ore.txhook.helper

import java.util.Locale

fun Short.toByteArray(): ByteArray = BytesUtil.int16ToBuf(this.toInt())

fun Int.toByteArray(): ByteArray = BytesUtil.int32ToBuf(this)

fun Long.toByteArray(): ByteArray = BytesUtil.int64ToBuf(this)

fun Int.toHexString(): String =
    (this and 0xFF).toString(16).padStart(2, '0').uppercase(Locale.getDefault())

fun Int.intToIp(): String = IpUtil.int_to_ip(this.toLong())

fun Long.longToIp(): String = IpUtil.int_to_ip(this)