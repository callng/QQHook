package moe.ore.xposed.hook

import moe.ore.xposed.utils.XPClassloader.load
import moe.ore.xposed.utils.hookMethod

internal object AntiDetection {

    operator fun invoke() {
        disableSwitch()
        // isLoginByNTHook()
    }

    private fun disableSwitch() {
        val configClass = load("com.tencent.freesia.UnitedConfig")
        configClass?.let {
            it.hookMethod("isSwitchOn")?.after { param ->
                val tag = param.args[1] as String
                when (tag) {
                    "msf_init_optimize", "msf_network_service_switch_new" -> {
                        param.result = false
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
}
