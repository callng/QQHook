package moe.ore.txhook.app

import android.os.Bundle
import android.widget.HorizontalScrollView
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.yuyh.jsonviewer.library.moved.ProtocolViewer
import moe.ore.android.EasyActivity
import moe.ore.android.toast.Toast
import moe.ore.txhook.R
import moe.ore.txhook.app.model.CapturePacket
import moe.ore.txhook.app.ui.theme.TxHookTheme
import moe.ore.txhook.helper.parser.ProtobufParser
import moe.ore.txhook.helper.parser.TarsParser
import moe.ore.txhook.helper.toByteReadPacket

class ParserActivity : EasyActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        val packet = intent.getParcelableExtra<CapturePacket>("data")
            ?: error("packet must not be null")
        val isJce = intent.getBooleanExtra("jce", false)

        val parsed = runCatching {
            val buffer = packet.buffer
            val index = buffer.toByteReadPacket().use {
                if (it.readInt() == buffer.size) 4 else 0
            }
            if (isJce) TarsParser(buffer, index).start() else ProtobufParser(buffer, index).start()
        }

        if (parsed.isFailure) {
            Toast.toast(msg = getString(R.string.parser_pb_fail))
            finish()
            return
        }

        val result = parsed.getOrThrow()

        setContent {
            TxHookTheme {
                ParserScreen(parsed = result, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParserScreen(parsed: Any, onBack: () -> Unit) {
    val bindDone = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analyse_jce_protobuf)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_ios_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { context ->
                    HorizontalScrollView(context).apply {
                        val parser = ProtocolViewer(context)
                        parser.setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
                        when (parsed) {
                            is org.json.JSONObject -> parser.bindJson(parsed)
                            is org.json.JSONArray -> parser.bindJson(parsed)
                            else -> parser.bindJson(parsed.toString())
                        }
                        bindDone.value = true
                        addView(parser)
                    }
                },
            )

            if (!bindDone.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.none_data))
                }
            }
        }
    }
}

private fun Int.dp(): Int = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()


