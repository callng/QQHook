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

import java.io.Closeable
import java.util.Timer
import java.util.TimerTask
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@JvmField
val EMPTY_BYTE_ARRAY = ByteArray(0)

@Throws(RuntimeException::class)
inline fun runtimeError(msg: String = "", th: Throwable? = null): Nothing =
    throw if (th == null) RuntimeException(msg) else RuntimeException(msg, th)

@OptIn(ExperimentalContracts::class)
inline fun <C : Closeable, R> C.withUse(block: C.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return use(block)
}

inline fun costTime(block: () -> Unit): Long {
    val st = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - st
}

fun timeoutEvent(time: Long, block: TimerTask.() -> Unit): Timer {
    val timer = Timer()
    timer.schedule(object : TimerTask() {
        override fun run() {
            this.block()
        }
    }, time)
    return timer
}

inline fun <C, R> C.fastTry(block: C.() -> R): Result<R> {
    return try {
        Result.success(block.invoke(this))
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

fun Closeable.closeQuietly() {
    kotlin.runCatching {
        close()
    }.onFailure { it.printStackTrace() }
}

inline fun <T> T?.ifNotNull(block: (T) -> Unit) {
    if (this != null) {
        block.invoke(this)
    }
    // block.invoke(this)
}

fun ByteArray?.hashCode(offset: Int): Int {
    if (this == null) return 0
    var result = 1
    for (i in offset until size) {
        result = 31 * result + this[i]
    }
    return result
}