@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package moe.ore.txhook.app.ui.compose

import android.os.Handler
import android.os.Looper
import android.widget.HorizontalScrollView
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yuyh.jsonviewer.library.moved.ProtocolViewer
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

private val uiThreadHandler = Handler(Looper.getMainLooper())

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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.none_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEach { (k, v) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = k,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(100.dp),
                        )
                        SelectionContainer {
                            Text(
                                text = v,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis,
                            )
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

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${buffer.size} bytes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { AndroidUtil.copyText(context, hex) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.action_copy),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                ) {
                    Text(
                        text = hex,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
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
    var isParsing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isParsing) return@Button
                    isParsing = true
                    thread(isDaemon = true) {
                        runCatching {
                            val buffer = packet.buffer
                            TarsParser(buffer, buffer.toByteReadPacket().use {
                                if (it.readInt() == buffer.size) 4 else 0
                            }).start()
                        }.onSuccess { result ->
                            uiThreadHandler.post {
                                parserHolder?.bindJson(result)
                                hasData = true
                                isParsing = false
                                Toast.toast(context, context.getString(R.string.parser_jce_ok))
                            }
                        }.onFailure {
                            uiThreadHandler.post {
                                isParsing = false
                                Toast.toast(context, context.getString(R.string.parser_jce_fail))
                            }
                        }
                    }
                },
                enabled = !isParsing,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = if (isParsing) "..." else stringResource(R.string.jce),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (isParsing) return@Button
                    isParsing = true
                    thread(isDaemon = true) {
                        runCatching {
                            val buffer = packet.buffer
                            ProtobufParser(buffer, buffer.toByteReadPacket().use {
                                if (it.readInt() == buffer.size) 4 else 0
                            }).start()
                        }.onSuccess { result ->
                            uiThreadHandler.post {
                                parserHolder?.bindJson(result)
                                hasData = true
                                isParsing = false
                                Toast.toast(context, context.getString(R.string.parser_pb_ok))
                            }
                        }.onFailure {
                            uiThreadHandler.post {
                                isParsing = false
                                Toast.toast(context, context.getString(R.string.parser_pb_fail))
                            }
                        }
                    }
                },
                enabled = !isParsing,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = if (isParsing) "..." else stringResource(R.string.protobuf),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            OutlinedButton(
                onClick = {
                    parserHolder?.bindJson(empty)
                    hasData = false
                    Toast.toast(context, context.getString(R.string.clear_success))
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = stringResource(R.string.clear_all),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
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

            androidx.compose.animation.AnimatedVisibility(
                visible = !hasData,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.DataObject,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.none_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun Int.dp(): Int =
    (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
