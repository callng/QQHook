package moe.ore.xposed.hooks

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import io.github.kyuubiran.ezxhelper.android.logging.Logger
import moe.ore.txhook.BuildConfig
import moe.ore.xposed.base.BaseHook
import moe.ore.xposed.base.MethodHook
import moe.ore.xposed.hook.base.hostContext
import kotlin.system.exitProcess

object ListenTXHookUpdate : BaseHook() {
    override val description: String get() = "ListenTXHookUpdate"

    @MethodHook
    private fun update() {
        val intent = IntentFilter()
        intent.addAction("android.intent.action.PACKAGE_ADDED")
        intent.addAction("android.intent.action.PACKAGE_REMOVED")
        intent.addAction("android.intent.action.PACKAGE_REPLACED")
        intent.addDataScheme("package")

        val companion = object: BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "android.intent.action.PACKAGE_ADDED",
                    "android.intent.action.PACKAGE_REMOVED",
                    "android.intent.action.PACKAGE_REPLACED" -> {
                        val packageName = intent.data?.schemeSpecificPart
                        if (packageName == BuildConfig.APPLICATION_ID) {
                            killRunningAppProcesses(hostContext)
                        }
                    }
                }
            }
        }

        hostContext.registerReceiver(companion, intent)
    }

    private fun killRunningAppProcesses(context: Context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = context.packageName
        val myPid = Process.myPid()

        val runningApps = activityManager.runningAppProcesses ?: return

        for (process in runningApps) {
            if (process.processName.startsWith(packageName)) {
                try {
                    Process.killProcess(process.pid)
                } catch (e: Exception) {
                    Logger.w("Failed to kill ${process.processName}", e)
                }
            }
        }

        if (Process.myPid() != myPid) return
        Process.killProcess(myPid)
        exitProcess(0)
    }
}
