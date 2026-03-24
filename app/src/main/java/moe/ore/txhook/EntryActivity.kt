package moe.ore.txhook

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.ore.android.AndroKtx
import moe.ore.android.EasyActivity
import moe.ore.txhook.app.MainActivity
import moe.ore.txhook.app.ui.theme.TxHookTheme
import moe.ore.xposed.common.ModeleStatus
import moe.ore.xposed.utils.PrefsManager
import kotlin.system.exitProcess

class EntryActivity : EasyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isModuleActivated = ModeleStatus.isModuleActivated()
        PrefsManager.initialize(applicationContext)
        val hasAgreedToTerms = PrefsManager.getBoolean(PrefsManager.KEY_AGREED_TO_TERMS)

        setContent {
            TxHookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    EntryScreen(
                        isModuleActivated = isModuleActivated,
                        hasAgreedToTerms = hasAgreedToTerms,
                        onAgree = {
                            PrefsManager.setBoolean(PrefsManager.KEY_AGREED_TO_TERMS, true)
                            AndroKtx.isInit = true
                            gotoMain()
                        },
                        onDisagree = { exitProcess(1) },
                    )
                }
            }
        }
    }

    private fun gotoMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
private fun EntryScreen(
    isModuleActivated: Boolean,
    hasAgreedToTerms: Boolean,
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
) {
    if (!isModuleActivated) {
        LaunchedEffect(Unit) {
            // 模块未激活，显示不可关闭的对话框
        }
        NotActivatedDialog(onDismiss = onDisagree)
    } else if (hasAgreedToTerms) {
        LaunchedEffect(Unit) {
            AndroKtx.isInit = true
            onAgree()
        }
    } else {
        WarningDialog(onAgree = onAgree, onDisagree = onDisagree)
    }
}

@Composable
private fun NotActivatedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "模块未激活",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = "请先在 XP/LSPosed 框架中激活本模块后再使用。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("确认")
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
private fun WarningDialog(onAgree: () -> Unit, onDisagree: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "使用警告",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "该软件仅供学习与交流使用，切勿用于违法领域，并请在24小时内删除！\n\n" +
                            "由于本软件的性质，使用本软件可能导致您的账号被封禁！继续使用则代表您已知晓该风险行为！\n\n" +
                            "如果您同意以上内容，请点击\"同意\"按钮，否则请点击\"不同意\"按钮并立即删除本软件！",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text("同意", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDisagree) {
                Text("不同意", color = MaterialTheme.colorScheme.error)
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
    )
}
