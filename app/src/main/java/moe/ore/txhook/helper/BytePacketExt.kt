/*
 * English :
 *  The project is protected by the MPL open source agreement.
 * Open source agreement warning that prohibits deletion of project source code files.
 * The project is prohibited from acting in illegal areas.
 * All illegal activities arising from the use of this project are the responsibility of the second author, and the original author of the project is not responsible
 *
 *  中文：
 *  该项目由MPL开源协议保护。
 *  禁止删除项目源代码文件的开源协议警告内容。
 * 禁止使用该项目在非法领域行事。
 * 使用该项目产生的违法行为，由使用者或第二作者全责，原作者免责
 *
 * 日本语：
 * プロジェクトはMPLオープンソース契約によって保護されています。
 *  オープンソース契約プロジェクトソースコードファイルの削除を禁止する警告。
 * このプロジェクトは違法地域の演技を禁止しています。
 * このプロジェクトの使用から生じるすべての違法行為は、2番目の著者の責任であり、プロジェクトの元の著者は責任を負いません。
 *
 */

package moe.ore.txhook.helper

import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.String
import kotlinx.io.core.readBytes
import kotlinx.io.core.readFully
import kotlinx.io.core.writeFully

fun newBuilder() = BytePacketBuilder()

/**
 * 转字节组
 * @receiver BytePacketBuilder
 * @return ByteArray
 */
fun BytePacketBuilder.toByteArray(): ByteArray {
    val reader = this.build()
    val array = ByteArray(reader.remaining.toInt())
    reader.readFully(array)
    return array
}

/**
 * 补充功能代码
 * @receiver BytePacketBuilder
 * @param packet BytePacketBuilder
 */
fun BytePacketBuilder.writePacket(packet: BytePacketBuilder) = this.writePacket(packet.build())

/**
 * 写布尔型
 * @receiver BytePacketBuilder
 * @param z Boolean
 */
fun BytePacketBuilder.writeBoolean(z: Boolean) = this.writeByte(if (z) 1 else 0)

/**
 * 自动转换类型
 * @receiver BytePacketBuilder
 * @param i Int
 */
fun BytePacketBuilder.writeShort(i: Int) = this.writeShort(i.toShort())

fun BytePacketBuilder.writeBytes(bytes: ByteArray) = this.writeFully(bytes)

fun BytePacketBuilder.writeLongToBuf32(v: Long) {
    this.writeBytes(BytesUtil.int64ToBuf32(v))
}

fun BytePacketBuilder.writeStringWithIntLen(str: String) {
    writeBytesWithIntLen(str.toByteArray())
}

fun BytePacketBuilder.writeStringWithShortLen(str: String) {
    writeBytesWithShortLen(str.toByteArray())
}

fun BytePacketBuilder.writeBytesWithIntLen(bytes: ByteArray) {
    writeInt(bytes.size)
    writeBytes(bytes)
}

fun BytePacketBuilder.writeBytesWithShortLen(bytes: ByteArray) {
    check(bytes.size <= Short.MAX_VALUE) { "byteArray length is too long" }
    writeShort(bytes.size.toShort())
    writeBytes(bytes)
}

inline fun BytePacketBuilder.writeBlockWithIntLen(
    len: (Int) -> Int = { it },
    block: BytePacketBuilder.() -> Unit
) {
    val builder = newBuilder()
    builder.block()
    this.writeInt(len(builder.size))
    this.writePacket(builder)
    builder.close()
}

inline fun BytePacketBuilder.writeBlockWithShortLen(
    len: (Int) -> Int = { it },
    block: BytePacketBuilder.() -> Unit
) {
    val builder = newBuilder()
    builder.block()
    this.writeShort(len(builder.size))
    this.writePacket(builder)
    builder.close()
}

fun BytePacketBuilder.md5(): ByteArray {
    return MD5.toMD5Byte(toByteArray())
}

inline fun BytePacketBuilder.writeTeaEncrypt(key: ByteArray, block: BytePacketBuilder.() -> Unit) {
    val body = newBuilder()
    body.block()
    this.writeBytes(TeaUtil.encrypt(body.toByteArray(), key))
    body.close()
}

fun BytePacketBuilder.writeString(str: String) {
    this.writeStringUtf8(str)
}

fun BytePacketBuilder.writeHex(uHex: String) {
    writeBytes(uHex.hex2ByteArray())
}

fun ByteReadPacket.readString(length: Int) = String(readBytes(length))

fun ByteArray.toByteReadPacket() = ByteReadPacket(this)

inline fun ByteArray.reader(block: ByteReadPacket.() -> Unit) {
    this.toByteReadPacket().block()
}

fun ByteReadPacket.readByteReadPacket(length: Int): ByteReadPacket {
    return readBytes(length).toByteReadPacket()
}
