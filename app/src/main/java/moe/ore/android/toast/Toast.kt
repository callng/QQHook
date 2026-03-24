package moe.ore.android.toast

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import moe.ore.android.AndroKtx
import moe.ore.android.Application

object Toast {

    /**
     * 普通toast
     */
    @JvmStatic
    fun toast(
        ctx: Context? = AndroKtx.context,
        msg: CharSequence?,
        mode: Int = Toast.LENGTH_SHORT
    ) {
        Application.uiHandler.post {
            Toast.makeText(ctx, msg, mode).show()
        }
    }

    @JvmStatic
    fun toast(
        ctx: Context? = AndroKtx.context,
        @StringRes resId: Int,
        mode: Int = Toast.LENGTH_SHORT
    ) {
        Application.uiHandler.post {
            Toast.makeText(ctx, resId, mode).show()
        }
    }
}