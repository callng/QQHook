package moe.ore.xposed.hook

import moe.ore.xposed.hook.base.hostPackageName
import moe.ore.xposed.hook.base.hostVersionCode
import moe.ore.xposed.hook.enums.QQTypeEnum
import moe.ore.xposed.utils.QQ_9_1_90_26520
import moe.ore.xposed.utils.QQ_9_2_10_29175
import moe.ore.xposed.utils.XPClassloader.load
import moe.ore.xposed.utils.hookMethod

internal object AntiDetection {

    operator fun invoke() {
        disableSwitch()
        isLoginByNTHook()
    }

    private fun disableSwitch() {
        val configClass = load("com.tencent.freesia.UnitedConfig")
        configClass?.let {
            it.hookMethod("isSwitchOn")?.after { param ->
                val tag = param.args[1] as String
                when (tag) {
                    "msf_init_optimize", "msf_network_service_switch_new" -> {
                        if (isSupportedDisablingNewService()) param.result = false
                    }
                    "wt_login_upgrade" -> {
                        param.result = false
                    }
                    "nt_login_downgrade" -> { // 强制降级到WT流程
                        param.result = true
                    }
                }
            }
        }
    }

    private fun isLoginByNTHook() {
        load("mqq.app.MobileQQ")?.hookMethod("isLoginByNT")?.after { param ->
            param.result = false
        }
    }

    fun isSupportedQQVersion(packageName: String, versionCode: Long): Boolean {
        return QQTypeEnum.valueOfPackage(packageName) == QQTypeEnum.QQ &&
                versionCode in QQ_9_1_90_26520..QQ_9_2_10_29175
    }

    fun isSupportedDisablingNewService(): Boolean {
        return (QQTypeEnum.valueOfPackage(hostPackageName) == QQTypeEnum.QQ &&
                hostVersionCode <= QQ_9_2_10_29175) ||
                QQTypeEnum.valueOfPackage(hostPackageName) == QQTypeEnum.TIM
    }
}
