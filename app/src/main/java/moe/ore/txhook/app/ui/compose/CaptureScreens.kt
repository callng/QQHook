@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package moe.ore.txhook.app.ui.compose

import android.widget.HorizontalScrollView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yuyh.jsonviewer.library.moved.ProtocolViewer
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.toast.Toast
import moe.ore.android.util.AndroidUtil
import moe.ore.txhook.R
import moe.ore.txhook.app.model.CapturePacket
import moe.ore.txhook.helper.parser.ProtobufParser
import moe.ore.txhook.helper.parser.TarsParser
import moe.ore.txhook.helper.toByteReadPacket
import moe.ore.txhook.helper.toHexString
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

@Composable
fun InfoSections(
    baseTitle: String,
    baseItems: List<Pair<String, String>>,
    extraTitle: String,
    extraItems: List<Pair<String, String>>,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 900.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoCard(title = baseTitle, items = baseItems, modifier = Modifier.weight(1f))
                InfoCard(title = extraTitle, items = extraItems, modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoCard(title = baseTitle, items = baseItems)
                InfoCard(title = extraTitle, items = extraItems)
            }
        }
    }
}

@Composable
fun InfoCard(title: String, items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            if (items.isEmpty()) {
                Text(text = stringResource(R.string.none_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEach { (k, v) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = k, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        SelectionContainer {
                            Text(text = v, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HexViewerCard(buffer: ByteArray, title: String = stringResource(R.string.hex)) {
    val context = LocalContext.current
    val hex = remember(buffer) { buffer.toHexString(true) }

    ElevatedCard(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { AndroidUtil.copyText(context, hex) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.action_copy))
                }
            }
            Text(
                text = stringResource(R.string.hex_info_format, buffer.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Text(
                    text = hex,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

@Composable
fun ParserToolCard(packet: CapturePacket) {
    val context = LocalContext.current
    val empty = remember { JSONObject(mapOf("empty" to true)) }
    var hasData by remember { mutableStateOf(false) }
    var parserHolder by remember { mutableStateOf<ProtocolViewer?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(modifier = Modifier.weight(1f), onClick = {
                    thread(isDaemon = true) {
                        runCatching {
                            val buffer = packet.buffer
                            TarsParser(buffer, buffer.toByteReadPacket().use {
                                if (it.readInt() == buffer.size) 4 else 0
                            }).start()
                        }.onSuccess {
                            parserHolder?.bindJson(it)
                            Toast.toast(context, context.getString(R.string.parser_jce_ok))
                        }.onFailure {
                            Toast.toast(context, context.getString(R.string.parser_jce_fail))
                        }
                    }
                }) {
                    Text(text = stringResource(R.string.jce))
                }
                Button(modifier = Modifier.weight(1f), onClick = {
                    thread(isDaemon = true) {
                        runCatching {
                            val buffer = packet.buffer
                            ProtobufParser(buffer, buffer.toByteReadPacket().use {
                                if (it.readInt() == buffer.size) 4 else 0
                            }).start()
                        }.onSuccess {
                            parserHolder?.bindJson(it)
                            Toast.toast(context, context.getString(R.string.parser_pb_ok))
                        }.onFailure {
                            Toast.toast(context, context.getString(R.string.parser_pb_fail))
                        }
                    }
                }) {
                    Text(text = stringResource(R.string.protobuf))
                }
                Button(onClick = {
                    parserHolder?.bindJson(empty)
                    Toast.toast(context, context.getString(R.string.clear_success))
                }) {
                    Text(text = stringResource(R.string.clear_all))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                HorizontalScrollView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    val parser = ProtocolViewer(ctx).apply {
                        setPadding(4.dp(), 4.dp(), 4.dp(), 4.dp())
                        setTextSize(16f)
                        setScaleEnable(true)
                        setOnBindListener(object : ProtocolViewer.OnBindListener {
                            override fun onBindString(json: String?) {
                                hasData = json != null
                            }

                            override fun onBindObject(json: JSONObject?) {
                                hasData = json?.optBoolean("empty", false) == false
                            }

                            override fun onBindArray(json: JSONArray?) {
                                hasData = json != null
                            }
                        })
                        bindJson(empty)
                    }
                    parserHolder = parser
                    addView(parser)
                }
            },
        )

        AnimatedVisibility(visible = !hasData, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.none_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
}

}
private fun Int.dp(): Int = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
