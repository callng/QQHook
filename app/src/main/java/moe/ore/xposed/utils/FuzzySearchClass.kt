package moe.ore.xposed.utils

import java.lang.reflect.Field
import java.lang.reflect.Method

object FuzzySearchClass {
    /**
     * QQ混淆字典
     */
    private val dic = arrayOf(
        "r", "t", "o", "a", "b", "c", "e", "f", "d", "g", "h", "i", "j",
        "k", "l", "m", "n", "p", "q", "s", "t", "u", "v", "w", "x", "y", "z"
    )
    private val rangePrefixes = ('a'..'h').map { "${it}0" }

    /**
     * 查找指定方法签名的混淆类
     *
     * @param classLoader 类加载器
     * @param packagePrefix 包前缀，如 "com.tencent.mobileqq.msf.core"
     * @param innerClassPath 嵌套类路径，如 "c.b\$e"
     * @param methodName 目标方法名
     * @param parameterTypes 方法参数类型
     * @return 符合条件的 Class
     */
    fun findClassWithMethod(
        classLoader: ClassLoader,
        packagePrefix: String,
        innerClassPath: String,
        methodName: String,
        parameterTypes: Array<Class<*>>
    ): Class<*>? {
        for (prefix in rangePrefixes) {
            val fullClassName = "$packagePrefix.$prefix.$innerClassPath"
            kotlin.runCatching {
                val clz = classLoader.loadClass(fullClassName)
                val method: Method? = clz.declaredMethods.firstOrNull {
                    it.name == methodName && it.parameterTypes.contentEquals(parameterTypes)
                }
                if (method != null) {
                    return clz
                }
            }
        }
        return null
    }

    fun findAllClassByField(
        classLoader: ClassLoader,
        prefix: String,
        check: (String, Field) -> Boolean
    ): List<Class<*>> {
        val list = arrayListOf<Class<*>>()
        dic.forEach { className ->
            kotlin.runCatching {
                val clz = classLoader.loadClass("$prefix.$className")
                clz?.declaredFields?.forEach {
                    if (!isBaseType(it.type) && check(className, it)) {
                        list.add(clz)
                    }
                }
            }
        }
        return list
    }

    private fun isBaseType(clz: Class<*>): Boolean {
        return clz == Long::class.java ||
                clz == Double::class.java ||
                clz == Float::class.java ||
                clz == Int::class.java ||
                clz == Short::class.java ||
                clz == Char::class.java ||
                clz == Byte::class.java
    }
}
