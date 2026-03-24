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

@SuppressWarnings("unchecked")
object DebugUtil {
    @JvmStatic
    fun getRunningThread(): Array<Thread> {
        return Thread.getAllStackTraces().keys.toTypedArray()
    }

    @JvmStatic
    fun <T> forcedConvert(any: Any?): T? {
        return any as? T
    }

    @JvmStatic
    fun <T> newInstance(clazz: Class<T>): T {
        return try {
            clazz.newInstance() as T
        } catch (e: Exception) {
            throw RuntimeException("instance Class: " + clazz.name + " with ex: " + e.message, e)
        }
    }

    @JvmStatic
    fun <T> newInstance(className: String): T {
        return try {
            Class.forName(className).newInstance() as T
        } catch (e: Exception) {
            throw RuntimeException("instance Class: " + className + " with ex: " + e.message, e)
        }
    }

    @JvmStatic
    fun forName(className: String): Class<*>? {
        return try {
            Class.forName(className)
        } catch (e: Exception) {
            throw RuntimeException("forName Class: " + className + " with ex: " + e.message, e)
        }
    }

    @JvmStatic
    fun isJavaBase(clazz: Class<*>): Boolean {
        return clazz == Boolean::class.javaPrimitiveType || clazz == Boolean::class.java || clazz == Byte::class.javaPrimitiveType || clazz == Byte::class.java || clazz == Short::class.javaPrimitiveType || clazz == Short::class.java || clazz == Int::class.javaPrimitiveType || clazz == Int::class.java || clazz == Long::class.javaPrimitiveType || clazz == Long::class.java || clazz == Float::class.javaPrimitiveType || clazz == Float::class.java || clazz == Double::class.javaPrimitiveType || clazz == Double::class.java || clazz == String::class.java
    }

    /**
     * 获取Cpu线程池大小
     * @return Int
     */
    @JvmStatic
    fun getCpuThreadPoolSize(): Int {
        val processors = Runtime.getRuntime().availableProcessors()
        return if (processors > 8) 4 + processors * 5 / 8 else processors + 1
    }

    /**
     * 获取IO线程池大小
     * @return Int
     */
    @JvmStatic
    fun getIoThreadPoolSize(): Int {
        val processors = Runtime.getRuntime().availableProcessors()
        return processors * 2
    }

    /**
     * 获取混合线程池大小
     * @return Int
     */
    @JvmStatic
    fun getMixThreadPoolSize() = (getCpuThreadPoolSize() + getIoThreadPoolSize()) / 2

}