package moe.ore.txhook

import android.content.Intent
import android.os.Bundle
import kotlin.system.exitProcess
import moe.ore.android.AndroKtx
import moe.ore.android.EasyActivity
import moe.ore.android.dialog.Dialog
import moe.ore.txhook.app.MainActivity
import moe.ore.xposed.common.ModeleStatus
import moe.ore.xposed.utils.PrefsManager

class EntryActivity : EasyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ModeleStatus.isModuleActivated()) {
            Dialog.CommonAlertBuilder(this)
                .setCancelable(false)
                .setTitle("\u6a21\u5757\u672a\u6fc0\u6d3b")
                .setMessage("\u8bf7\u5148\u5728 XP/LSPosed \u6846\u67b6\u4e2d\u6fc0\u6d3b\u672c\u6a21\u5757\u540e\u518d\u4f7f\u7528\u3002")
                .setNegativeButton(getString(R.string.confirm)) { dialog, _ ->
                    dialog.dismiss()
                    exitProcess(1)
                }
                .show()
        } else {
            PrefsManager.initialize(applicationContext)
            val hasAgreedToTerms = PrefsManager.getBoolean(PrefsManager.KEY_AGREED_TO_TERMS)

            if (hasAgreedToTerms) {
                AndroKtx.isInit = true
                gotoMain()
            } else {
                Dialog.CommonAlertBuilder(this)
                    .setCancelable(false)
                    .setTitle("\u4f7f\u7528\u8b66\u544a")
                    .setMessage(
                        "\u8be5\u8f6f\u4ef6\u4ec5\u4f9b\u5b66\u4e60\u4e0e\u4ea4\u6d41\u4f7f\u7528\uff0c\u5207\u52ff\u7528\u4e8e\u8fdd\u6cd5\u9886\u57df\uff0c\u5e76\u8bf7\u572824\u5c0f\u65f6\u5185\u5220\u9664\uff01\n\n" +
                            "\u7531\u4e8e\u672c\u8f6f\u4ef6\u7684\u6027\u8d28\uff0c\u4f7f\u7528\u672c\u8f6f\u4ef6\u53ef\u80fd\u5bfc\u81f4\u60a8\u7684\u8d26\u53f7\u88ab\u5c01\u7981\uff01\u7ee7\u7eed\u4f7f\u7528\u5219\u4ee3\u8868\u60a8\u5df2\u77e5\u6653\u8be5\u98ce\u9669\u884c\u4e3a\uff01\n\n" +
                            "\u5982\u679c\u60a8\u540c\u610f\u4ee5\u4e0a\u5185\u5bb9\uff0c\u8bf7\u70b9\u51fb\u201c\u540c\u610f\u201d\u6309\u94ae\uff0c\u5426\u5219\u8bf7\u70b9\u51fb\u201c\u4e0d\u540c\u610f\u201d\u6309\u94ae\u5e76\u7acb\u5373\u5220\u9664\u672c\u8f6f\u4ef6\uff01"
                    )
                    .setPositiveButton("\u540c\u610f") { dialog, _ ->
                        dialog.dismiss()
                        PrefsManager.setBoolean(PrefsManager.KEY_AGREED_TO_TERMS, true)
                        AndroKtx.isInit = true
                        gotoMain()
                    }
                    .setNegativeButton("\u4e0d\u540c\u610f") { dialog, _ ->
                        dialog.dismiss()
                        exitProcess(1)
                    }
                    .show()
            }
        }
    }

    private fun gotoMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
