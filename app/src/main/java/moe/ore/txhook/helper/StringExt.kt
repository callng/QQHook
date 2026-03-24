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

private val HEX_LOOKUP = IntArray(256) { -1 }.apply {
    for (i in '0'..'9') this[i.code] = i - '0'
    for (i in 'A'..'F') this[i.code] = i - 'A' + 10
    for (i in 'a'..'f') this[i.code] = i - 'a' + 10
}

fun String.hex2ByteArray(stripWhitespace: Boolean = false): ByteArray {
    val hex = if (stripWhitespace) this.filterNot { it <= ' ' } else this
    val length = hex.length
    require(length and 1 == 0) { "Hex string length must be even" }

    val result = ByteArray(length shr 1)  // length / 2
    var i = 0
    var j = 0
    while (i < length) {
        val hi = HEX_LOOKUP[hex[i++].code]
        val lo = HEX_LOOKUP[hex[i++].code]
        if ((hi or lo) < 0) {  // 合并非法字符检查
            throw IllegalArgumentException("Invalid hex char at pos ${i - 2} or ${i - 1}")
        }
        result[j++] = ((hi shl 4) or lo).toByte()
    }
    return result
}

fun String.ipToLong(): Int = IpUtil.ip_to_int(this)