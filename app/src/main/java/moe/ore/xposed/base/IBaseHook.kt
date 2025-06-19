package moe.ore.xposed.base

interface IBaseHook {
    fun init() {}
    val enabled: Boolean
        get() = true
    val isCompatible: Boolean
        get() = true
    val description: String
        get() = "default description"
}
