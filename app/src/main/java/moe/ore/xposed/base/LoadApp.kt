package moe.ore.xposed.base

import android.app.Application
import android.content.Context
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.kyuubiran.ezxhelper.android.logging.Logger
import moe.ore.txhook.BuildConfig
import moe.ore.xposed.common.ModeleStatus
import moe.ore.xposed.hook.AntiDetection
import moe.ore.xposed.hook.MainHook
import moe.ore.xposed.hook.base.ProcUtil
import moe.ore.xposed.hook.base.hostAndroidId
import moe.ore.xposed.hook.base.hostApp
import moe.ore.xposed.hook.base.hostAppName
import moe.ore.xposed.hook.base.hostClassLoader
import moe.ore.xposed.hook.base.hostInit
import moe.ore.xposed.hook.base.hostPackageName
import moe.ore.xposed.hook.base.hostProcessName
import moe.ore.xposed.hook.base.hostVersionCode
import moe.ore.xposed.hook.base.hostVersionName
import moe.ore.xposed.hook.base.moduleClassLoader
import moe.ore.xposed.hook.enums.QQTypeEnum
import moe.ore.xposed.hooks.ListenTXHookUpdate
import moe.ore.xposed.utils.FuzzySearchClass
import moe.ore.xposed.utils.XPClassloader
import moe.ore.xposed.utils.afterHook
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object LoadApp {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (QQTypeEnum.valueOfPackage(lpparam.packageName)) {
            QQTypeEnum.TXHook -> hookActivation(lpparam)
            else -> hookEntry(lpparam)
        }
    }

    private fun hookActivation(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            ModeleStatus::class.java.name,
            lpparam.classLoader,
            "isModuleActivated",
            XC_MethodReplacement.returnConstant(true)
        )
    }

    private fun hookEntry(lpparam: XC_LoadPackage.LoadPackageParam) {
        hostPackageName = lpparam.packageName
        hostProcessName = lpparam.processName
        hostClassLoader = lpparam.classLoader

        val startup = afterHook(50) { param ->
            try {
                val loader = param.thisObject.javaClass.classLoader!!
                XPClassloader.ctxClassLoader = loader

                val clz = try {
                    loader.loadClass("com.tencent.common.app.BaseApplicationImpl")
                } catch (e: ClassNotFoundException) {
                    Logger.e("loadClass BaseApplicationImpl failed", e)
                    loader.loadClass("com.tencent.qqnt.watch.app.WatchApplicationDelegate")
                }
                val field = clz.declaredFields.first {
                    it.type == clz
                }

                val app: Context = field.get(null) as Context
                if (!hostInit) {
                    hostApp = app as Application
                }
                hostClassLoader = hostApp.classLoader

                execStartupInit(app)

                if (ProcUtil.isMain) {
                    Logger.i("""
                        module version: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})
                        hook version: ${hostAppName}-$hostVersionName($hostVersionCode)
                        androidId: $hostAndroidId
                    """.trimIndent())
                }
            } catch (e: Exception) {
                Logger.e("HookEntry startup failed", e)
            }
        }

        entry(hostClassLoader, startup)
    }

    private fun classExists(loader: ClassLoader, className: String): Boolean =
        try {
            loader.loadClass(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }

    private fun hookBooleanNoParamMethods(loader: ClassLoader, className: String, hook: XC_MethodHook) {
        val clazz = loader.loadClass(className)
        clazz.declaredMethods
            .filter { it.returnType == Boolean::class.java && it.parameterTypes.isEmpty() }
            .forEach {
                XposedBridge.hookMethod(it, hook)
            }
    }

    private fun handleNtQqHook(loader: ClassLoader, hook: XC_MethodHook) {
        val fieldList = arrayListOf<Field>()
        FuzzySearchClass.findAllClassByField(loader, "com.tencent.mobileqq.startup.task.config") { _, field ->
            (field.type == HashMap::class.java || field.type == Map::class.java) && Modifier.isStatic(field.modifiers)
        }.forEach {
            it.declaredFields.forEach { field ->
                if ((field.type == HashMap::class.java || field.type == Map::class.java) && Modifier.isStatic(field.modifiers)) {
                    fieldList.add(field)
                }
            }
        }

        fieldList.forEach { field ->
            if (!field.isAccessible) field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field.get(null) as? Map<String, Class<*>>)?.forEach { (key, clazz) ->
                if (key.contains("LoadDex", ignoreCase = true)) {
                    clazz.declaredMethods.forEach { method ->
                        if (method.parameterTypes.size == 1 && method.parameterTypes[0] == Context::class.java) {
                            XposedBridge.hookMethod(method, hook)
                        }
                    }
                }
            }
        }
    }

    private fun entry(loader: ClassLoader, startup: XC_MethodHook) {
        val type = when {
            classExists(loader, "com.tencent.mobileqq.startup.step.LoadDex") -> 1
            classExists(loader, "com.tencent.qqnt.watch.startup.task.ApplicationCreateStageTask") -> 2
            else -> 0
        }

        when (type) {
            0 -> handleNtQqHook(loader, startup)
            1 -> hookBooleanNoParamMethods(loader, "com.tencent.mobileqq.startup.step.LoadDex", startup)
            2 -> hookBooleanNoParamMethods(loader, "com.tencent.qqnt.watch.startup.task.ApplicationCreateStageTask", startup)
        }
    }

    private fun execStartupInit(ctx: Context) {
        val classLoader = ctx.classLoader.also { requireNotNull(it) }
        XPClassloader.hostClassLoader = classLoader

        initHooks(
            // list
            ListenTXHookUpdate,
        )

        if (injectClassloader(moduleClassLoader)) {
            // TODO 先暂时用原来的，后面再改
            if (ProcUtil.isMsf) {
                AntiDetection()
                MainHook(0, ctx)
            }
        }
    }

    private fun injectClassloader(moduleLoader: ClassLoader): Boolean {
        if (runCatching { moduleLoader.loadClass("mqq.app.MobileQQ") }.isSuccess) {
            return true
        }

        val parent = moduleLoader.parent
        val field = ClassLoader::class.java.declaredFields
            .first { it.name == "parent"}

        field.isAccessible = true
        field.set(XPClassloader, parent)

        if (XPClassloader.load("mqq.app.MobileQQ") == null) return false
        field.set(moduleLoader, XPClassloader)

        return runCatching { Class.forName("mqq.app.MobileQQ") }.isSuccess
    }

    private fun initHook(hooks: List<BaseHook>) {
        hooks.forEach {
            kotlin.runCatching {
                it.init()
            }.onFailure {
                Logger.e("Hook init failed", it)
            }
        }
        Logger.i("Hook init success")
    }

    private fun initHooks(vararg hooks: BaseHook) {
        for (hook in hooks) {
            kotlin.runCatching {
                hook.init()
            }.onFailure {
                Logger.e("Hook init failed", it)
            }
        }
        Logger.i("Hook init success")
    }
}
