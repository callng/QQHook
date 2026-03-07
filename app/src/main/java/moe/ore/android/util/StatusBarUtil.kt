package moe.ore.android.util

import android.R
import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt

object StatusBarUtil {
    fun setAndroidNativeLightStatusBar(activity: Activity, dark: Boolean, isUseFullScreenMode: Boolean = true) {
        val decor: View = activity.window.decorView
        if (dark) {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        val content = (activity.findViewById<ViewGroup>(R.id.content)!!).getChildAt(0)
        if (content != null && !isUseFullScreenMode) {
            content.fitsSystemWindows = true
        }
    }

    fun transparentStatusBar(activity: Activity) {
        val window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    }

    fun setStatusBarColor(activity: Activity, @ColorInt color: Int) {
        val window: Window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
    }
}
