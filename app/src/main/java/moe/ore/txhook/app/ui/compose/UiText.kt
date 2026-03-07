package moe.ore.txhook.app.ui.compose

import moe.ore.txhook.R
import moe.ore.txhook.app.model.CaptureAction
import moe.ore.txhook.app.model.SourceApp

fun sourceIcon(source: Int): Int = when (source) {
    SourceApp.TIM -> R.drawable.icon_tim
    else -> R.drawable.icon_mobileqq
}

fun sourceName(source: Int): String = when (source) {
    SourceApp.MQQ -> "\u624b\u673aQQ"
    SourceApp.TIM -> "TIM"
    SourceApp.QQLITE -> "QQ\u8f7b\u804a\u7248"
    SourceApp.QIDIAN -> "QIDIAN"
    else -> "\u672a\u77e5"
}

fun actionTitle(action: CaptureAction): String = when (action.type) {
    0 -> "Tea \u52a0\u5bc6"
    1 -> "Tea \u89e3\u5bc6"
    2 -> "T${Integer.toHexString(action.what)} ${if (action.from) "\u89e3\u5bc6" else "\u52a0\u5bc6"}"
    3 -> "MD5"
    else -> "\u672a\u77e5"
}