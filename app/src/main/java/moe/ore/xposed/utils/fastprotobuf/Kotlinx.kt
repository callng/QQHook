package moe.ore.xposed.utils.fastprotobuf

import java.util.*

@JvmOverloads fun ByteArray?.toHexString(uppercase: Boolean = false): String = this?.joinToString("") {
    (it.toInt() and 0xFF).toString(16)
        .padStart(2, '0')
        .let { s -> if (uppercase) s.lowercase(Locale.getDefault()) else s }
} ?: "null"

// From Kotlin1.9, You can use the kotlin library function: String.hexToByteArray()
@JvmOverloads fun String.hex2ByteArray(replace: Boolean = false): ByteArray {
    val s = if (replace) this.replace(" ", "")
        .replace("\n", "")
        .replace("\t", "")
        .replace("\r", "") else this
    val bs = ByteArray(s.length / 2)
    for (i in 0 until s.length / 2) {
        bs[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
    return bs
}
