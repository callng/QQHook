package moe.ore.xposed.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import de.robv.android.xposed.XposedBridge.log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.random.Random

@Suppress("DEPRECATION")
fun getAppVersionCode(context: Context, packageName: String): Long {
    runCatching {
        val pi = context.packageManager.getPackageInfo(packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pi.longVersionCode
        } else {
            pi.versionCode.toLong()
        }
    }.onFailure { log(it) }

    return -1
}

fun getAppVersionName(context: Context, packageName: String): String {
    runCatching {
        val pi = context.packageManager.getPackageInfo(packageName, 0)
        return pi.versionName ?: "unknown"
    }.onFailure { log(it) }

    return "unknown"
}

fun getAppName(context: Context, packageName: String): String {
    runCatching {
        val pm = context.packageManager
        val pi = pm.getApplicationInfo(packageName, 0)
        return pm.getApplicationLabel(pi).toString()
    }.onFailure { log(it) }

    return "unknown"
}

fun getAppIcon(context: Context, packageName: String): Bitmap? {
    runCatching {
        val bd = context.packageManager.getApplicationIcon(packageName) as BitmapDrawable
        return bd.bitmap
    }.onFailure { log(it) }

    return null
}

fun getTextFromAssets(classLoader: ClassLoader, fileName: String): String {
    runCatching {
        classLoader.getResourceAsStream("assets/$fileName").use { stream ->
            val bis = BufferedInputStream(stream)
            val bos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            while (bis.read(buffer).also { len = it } != -1) {
                bos.write(buffer, 0, len)
            }
            return String(bos.toByteArray(), StandardCharsets.UTF_8)
        }
    }.onFailure { log(it) }

    return ""
}

fun getArtApexVersion(context: Context): Long {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return Build.VERSION.SDK_INT.toLong()
    }

    try {
        val packageManager = context.packageManager
        val info =
            packageManager.getPackageInfo("com.google.android.art", PackageManager.MATCH_APEX)
        return info.longVersionCode
    } catch (_: Throwable) {
        return -1
    }
}

fun <T> runRetry(retryNum: Int, sleepMs: Long = 0, block: () -> T?): T? {
    for (i in 1..retryNum) {
        runCatching {
            return block()
        }.onFailure {
            log(it)
        }
        if (sleepMs > 0) Thread.sleep(sleepMs)
    }

    return null
}

fun killProcess(ctx: Context, packageName: String) {
    for (processInfo in (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses) {
        if (processInfo.processName == packageName) {
            Process.killProcess(processInfo.pid)
            break
        }
    }
}

@SuppressLint("HardwareIds")
fun getAndroidId(ctx: Context): String {
    val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
    if (androidId == null) {
        val sb = StringBuilder()
        for (i in 0..15) {
            sb.append(Random.nextInt(10))
        }
        return sb.toString()
    }

    return androidId
}
