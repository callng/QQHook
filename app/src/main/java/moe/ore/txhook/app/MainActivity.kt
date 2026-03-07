@file:OptIn(ExperimentalSerializationApi::class)

package moe.ore.txhook.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yuyh.jsonviewer.library.moved.ProtocolViewer
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.AndroKtx
import moe.ore.android.EasyActivity
import moe.ore.android.toast.Toast.toast
import moe.ore.script.Consist
import moe.ore.txhook.EntryActivity
import moe.ore.txhook.R
import moe.ore.txhook.app.model.CaptureAction
import moe.ore.txhook.app.model.CapturePacket
import moe.ore.txhook.app.model.CaptureRepository
import moe.ore.txhook.app.ui.compose.actionTitle
import moe.ore.txhook.app.ui.compose.sourceIcon
import moe.ore.txhook.app.ui.compose.sourceName
import moe.ore.txhook.app.ui.theme.TxHookTheme
import moe.ore.txhook.helper.FormatUtil
import moe.ore.xposed.utils.PrefsManager
import moe.ore.xposed.utils.PrefsManager.KEY_PUSH_API
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : EasyActivity() {
    private var isExit = 0
    private val exitHandler: Handler by lazy {
        object : Handler(mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                isExit--
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!AndroKtx.isInit) {
            startActivity(Intent(this, EntryActivity::class.java))
            finish()
            return
        }

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

        CaptureRepository.attachCatchProvider()

        val widthSizeClass = when (resources.configuration.screenWidthDp) {
            in Int.MIN_VALUE..599 -> WindowWidthSizeClass.Compact
            in 600..839 -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
        setContent {
            TxHookTheme {
                MainScreen(
                    widthSizeClass = widthSizeClass,
                    onOpenPacket = { openPacket(it) },
                    onOpenAction = { openAction(it) },
                )
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isExit++
            if (isExit < 2) {
                toast(msg = getString(R.string.press_again_exit))
                exitHandler.sendEmptyMessageDelayed(0, 2000)
            } else {
                finish()
                super.onBackPressed()
            }
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun openPacket(packet: CapturePacket) {
        startActivity(Intent(this, PacketActivity::class.java).putExtra("data", packet))
    }

    private fun openAction(action: CaptureAction) {
        val activityClass = when (action.type) {
            2 -> TlvActivity::class.java
            3 -> Md5Activity::class.java
            else -> TeaActivity::class.java
        }
        startActivity(Intent(this, activityClass).putExtra("data", action))
    }
}

@Composable
private fun MainScreen(
    widthSizeClass: WindowWidthSizeClass,
    onOpenPacket: (CapturePacket) -> Unit,
    onOpenAction: (CaptureAction) -> Unit,
) {
    var mainTab by rememberSaveable { mutableIntStateOf(0) }
    var modeTab by rememberSaveable { mutableIntStateOf(0) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val packets = CaptureRepository.packets
    val actions = CaptureRepository.actions

    val listPadding = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 28.dp
        WindowWidthSizeClass.Medium -> 20.dp
        else -> 14.dp
    }

    val filteredPackets = remember(packets.size, searchText) {
        if (searchText.isBlank()) packets.toList()
        else packets.filter { it.cmd.contains(searchText, ignoreCase = true) }
    }
    val filteredActions = remember(actions.size, searchText) {
        if (searchText.isBlank()) actions.toList()
        else actions.filter { actionTitle(it).contains(searchText, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(start = listPadding, end = listPadding, top = 8.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.imqq),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                TabRow(selectedTabIndex = mainTab) {
                    Tab(
                        selected = mainTab == 0,
                        onClick = { mainTab = 0 },
                        text = { Text(stringResource(R.string.tab_home)) },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    )
                    Tab(
                        selected = mainTab == 1,
                        onClick = { mainTab = 1 },
                        text = { Text(stringResource(R.string.tab_settings)) },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = mainTab == 0, enter = fadeIn(), exit = fadeOut()) {
                val enabled = CaptureRepository.isCatchEnabled
                val tint by animateFloatAsState(if (enabled) 1f else 0.75f, label = "fab_tint")
                FloatingActionButton(
                    onClick = { CaptureRepository.updateCatchEnabled(!enabled) },
                    containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        painter = painterResource(if (enabled) R.drawable.icon_catch else R.drawable.icon_nocatch),
                        contentDescription = stringResource(R.string.start_catch),
                        modifier = Modifier.size((24 * tint).dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(targetState = mainTab, label = "main_tab") { selected ->
            when (selected) {
                0 -> HomeTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalPadding = listPadding,
                    modeTab = modeTab,
                    onModeTabChange = { modeTab = it },
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    packets = filteredPackets,
                    actions = filteredActions,
                    onClear = {
                        if (modeTab == 0) CaptureRepository.clearPackets() else CaptureRepository.clearActions()
                    },
                    onOpenPacket = onOpenPacket,
                    onOpenAction = onOpenAction,
                )

                else -> SettingsTab(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalPadding = listPadding,
                )
            }
        }
    }
}

@Composable
private fun HomeTab(
    modifier: Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modeTab: Int,
    onModeTabChange: (Int) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    packets: List<CapturePacket>,
    actions: List<CaptureAction>,
    onClear: () -> Unit,
    onOpenPacket: (CapturePacket) -> Unit,
    onOpenAction: (CaptureAction) -> Unit,
) {
    val entries = if (modeTab == 0) packets else actions
    val listState = rememberLazyListState()
    var previousCount by remember(modeTab) { mutableIntStateOf(entries.size) }
    var followLatest by remember(modeTab) { mutableStateOf(true) }

    LaunchedEffect(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset,
        listState.isScrollInProgress,
    ) {
        val isAtLatest = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        if (isAtLatest) {
            followLatest = true
        } else if (listState.isScrollInProgress) {
            followLatest = false
        }
    }

    LaunchedEffect(entries.size, searchText, modeTab) {
        if (searchText.isBlank() && followLatest && entries.size > previousCount) {
            listState.animateScrollToItem(0)
        }
        previousCount = entries.size
    }

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding, vertical = 10.dp),
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.input_search)) },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(10.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.catch_content),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = modeTab == 0,
                    onClick = { onModeTabChange(0) },
                    label = { Text(stringResource(R.string.capture_mode_sso)) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = modeTab == 1,
                    onClick = { onModeTabChange(1) },
                    label = { Text(stringResource(R.string.capture_mode_action)) },
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_all))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedVisibility(visible = entries.isNotEmpty(), modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (modeTab == 0) {
                    items(packets, key = { "p-${it.time}-${it.seq}-${it.cmd}" }) { packet ->
                        PacketCard(packet = packet, onClick = { onOpenPacket(packet) })
                    }
                } else {
                    items(actions, key = { "a-${it.time}-${it.type}-${it.what}" }) { action ->
                        ActionCard(action = action, onClick = { onOpenAction(action) })
                    }
                }
            }
        }

        AnimatedVisibility(visible = entries.isEmpty(), modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.start_catch),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PacketCard(packet: CapturePacket, onClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = sourceIcon(packet.source)),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(6.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packet.cmd,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = packet.uin.toString(), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = packet.seq.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = if (packet.from) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                    contentDescription = null,
                )
                Text(text = formatTime(packet.time), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = FormatUtil.formatFileSize(packet.buffer.size.toLong()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ActionCard(action: CaptureAction, onClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = sourceIcon(action.source)),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(6.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actionTitle(action),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = sourceName(action.source), color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = if (action.from) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                    contentDescription = null,
                )
                Text(text = formatTime(action.time), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = FormatUtil.formatFileSize(action.buffer.size.toLong()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingsTab(modifier: Modifier, horizontalPadding: androidx.compose.ui.unit.Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var address by remember { mutableStateOf(PrefsManager.getString(KEY_PUSH_API)) }
    var checked by remember { mutableStateOf(address.isNotBlank()) }
    var showDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf(address.ifBlank { context.getString(R.string.local_address) }) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                if (!PrefsManager.getString(KEY_PUSH_API).isNullOrBlank()) {
                    checked = true
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalText = editingAddress.ifBlank { context.getString(R.string.local_address) }
                    PrefsManager.setString(KEY_PUSH_API, finalText)
                    address = finalText
                    checked = true
                    showDialog = false
                    toast(context, context.getString(R.string.push_configured))
                }) {
                    Text(text = stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    checked = PrefsManager.getString(KEY_PUSH_API).isNotBlank()
                }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            title = { Text(stringResource(R.string.input_target_address)) },
            text = {
                OutlinedTextField(
                    value = editingAddress,
                    onValueChange = { editingAddress = it },
                    label = { Text(stringResource(R.string.input_domain_or_ip)) },
                    singleLine = true,
                )
            },
        )
    }

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding, vertical = 16.dp),
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pushapi),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = address.ifBlank { stringResource(R.string.not_configured) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = checked,
                    onCheckedChange = { enabled ->
                        checked = enabled
                        if (enabled) {
                            editingAddress = address.ifBlank { context.getString(R.string.local_address) }
                            showDialog = true
                        } else {
                            PrefsManager.setString(KEY_PUSH_API, "")
                            address = ""
                            toast(context, context.getString(R.string.push_disabled))
                        }
                    },
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(Date(timestamp))
}




