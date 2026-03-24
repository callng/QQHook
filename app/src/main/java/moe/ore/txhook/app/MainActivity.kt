@file:OptIn(ExperimentalSerializationApi::class)

package moe.ore.txhook.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.AndroKtx
import moe.ore.android.EasyActivity
import moe.ore.android.toast.Toast.toast
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
        try {
            startActivity(Intent(this, PacketActivity::class.java).putExtra("data", packet))
        } catch (e: Exception) {
            toast(msg = "打开失败: ${e.message}")
        }
    }

    private fun openAction(action: CaptureAction) {
        try {
            val activityClass = when (action.type) {
                2 -> TlvActivity::class.java
                3 -> Md5Activity::class.java
                else -> TeaActivity::class.java
            }
            startActivity(Intent(this, activityClass).putExtra("data", action))
        } catch (e: Exception) {
            toast(msg = "打开失败: ${e.message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    widthSizeClass: WindowWidthSizeClass,
    onOpenPacket: (CapturePacket) -> Unit,
    onOpenAction: (CaptureAction) -> Unit,
) {
    var mainTab by rememberSaveable { mutableIntStateOf(0) }

    val listPadding = when (widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 28.dp
        WindowWidthSizeClass.Medium -> 20.dp
        else -> 14.dp
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.imqq),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {},
                actions = {},
                scrollBehavior = scrollBehavior,
            )
        },

        floatingActionButton = {
            if (mainTab == 0) {
                val enabled = CaptureRepository.isCatchEnabled
                FloatingActionButton(
                    onClick = { CaptureRepository.updateCatchEnabled(!enabled) },
                    containerColor = if (enabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (enabled)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(
                        painter = painterResource(if (enabled) R.drawable.icon_catch else R.drawable.icon_nocatch),
                        contentDescription = stringResource(R.string.start_catch),
                    )
                }
            }
        },
        bottomBar = {
            TabRow(
                selectedTabIndex = mainTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Tab(
                    selected = mainTab == 0,
                    onClick = { mainTab = 0 },
                    text = { Text(stringResource(R.string.tab_home)) },
                    icon = {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Tab(
                    selected = mainTab == 1,
                    onClick = { mainTab = 1 },
                    text = { Text(stringResource(R.string.tab_settings)) },
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTab(
    modifier: Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onOpenPacket: (CapturePacket) -> Unit,
    onOpenAction: (CaptureAction) -> Unit,
) {
    var modeTab by rememberSaveable { mutableIntStateOf(0) }
    var searchText by rememberSaveable { mutableStateOf("") }
    val allPackets = CaptureRepository.packets
    val allActions = CaptureRepository.actions

    // derivedStateOf: 列表变化或搜索词变化都自动重新过滤，搜索时新数据不丢失
    val filteredPackets by remember {
        derivedStateOf {
            val q = searchText.trim()
            if (q.isBlank()) allPackets
            else allPackets.filter { pkt ->
                pkt.cmd.contains(q, ignoreCase = true) ||
                pkt.seq.toString().contains(q) ||
                pkt.uin.toString().contains(q)
            }
        }
    }
    val filteredActions by remember {
        derivedStateOf {
            val q = searchText.trim()
            if (q.isBlank()) allActions
            else allActions.filter { act ->
                actionTitle(act).contains(q, ignoreCase = true) ||
                act.what.toString().contains(q) ||
                sourceName(act.source).contains(q, ignoreCase = true)
            }
        }
    }

    val entries = if (modeTab == 0) filteredPackets else filteredActions
    val totalCount = if (modeTab == 0) allPackets.size else allActions.size
    val isSearching = searchText.isNotBlank()

    val listState = rememberLazyListState()
    var previousCount by remember(modeTab) { mutableIntStateOf(entries.size) }
    var followLatest by remember(modeTab) { mutableStateOf(true) }

    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) followLatest = true
    }

    LaunchedEffect(entries.size, searchText, modeTab) {
        if (!isSearching && followLatest && entries.size > previousCount) {
            listState.animateScrollToItem(0)
        }
        previousCount = entries.size
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.ROOT) }

    // 搜索框焦点管理：点击列表区域自动收起键盘
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .padding(horizontal = horizontalPadding, vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus() },
            ),
    ) {
        if (modeTab == 0) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索 CMD / SEQ / UIN") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                SegmentedButton(
                    selected = modeTab == 0,
                    onClick = {
                        focusManager.clearFocus()
                        modeTab = 0
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(
                        text = stringResource(R.string.capture_mode_sso),
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (allPackets.isNotEmpty()) {
                                    CaptureRepository.clearPackets()
                                }
                            },
                        ),
                    )
                }
                SegmentedButton(
                    selected = modeTab == 1,
                    onClick = {
                        focusManager.clearFocus()
                        modeTab = 1
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(
                        text = stringResource(R.string.capture_mode_action),
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (allActions.isNotEmpty()) {
                                    CaptureRepository.clearActions()
                                }
                            },
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = if (modeTab == 0 && isSearching) "${entries.size}/${totalCount}"
                       else "${totalCount}",
                style = MaterialTheme.typography.labelMedium,
                color = if (modeTab == 0 && isSearching)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                if (modeTab == 0) {
                    items(
                        items = filteredPackets,
                        key = { it.uid },
                    ) { packet ->
                        PacketCard(
                            packet = packet,
                            onClick = { onOpenPacket(packet) },
                            timeFormat = timeFormat,
                        )
                    }
                } else {
                    items(
                        items = filteredActions,
                        key = { it.uid },
                    ) { action ->
                        ActionCard(
                            action = action,
                            onClick = { onOpenAction(action) },
                            timeFormat = timeFormat,
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (isSearching) Icons.Rounded.Search else Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isSearching) "无匹配结果" else stringResource(R.string.start_catch),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PacketCard(packet: CapturePacket, onClick: () -> Unit, timeFormat: SimpleDateFormat) {
    val isDark = isSystemInDarkTheme()
    val directionColor = if (packet.from)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark)
                MaterialTheme.colorScheme.surfaceContainerLow
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDark) 0.dp else 1.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = sourceIcon(packet.source)),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(5.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        imageVector = if (packet.from) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(directionColor),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = packet.cmd,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "UIN:${packet.uin}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "SEQ:${packet.seq}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeFormat.format(Date(packet.time)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FormatUtil.formatFileSize(packet.buffer.size.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionCard(action: CaptureAction, onClick: () -> Unit, timeFormat: SimpleDateFormat) {
    val isDark = isSystemInDarkTheme()
    val directionColor = if (action.from)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark)
                MaterialTheme.colorScheme.surfaceContainerLow
            else
                MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDark) 0.dp else 1.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = sourceIcon(action.source)),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(5.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        imageVector = if (action.from) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(directionColor),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = actionTitle(action),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sourceName(action.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeFormat.format(Date(action.time)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = FormatUtil.formatFileSize(action.buffer.size.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    var editingAddress by remember {
        mutableStateOf(address.ifBlank { context.getString(R.string.local_address) })
    }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
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
                    shape = MaterialTheme.shapes.medium,
                )
            },
            shape = MaterialTheme.shapes.extraLarge,
        )
    }

    Column(
        modifier = modifier.padding(horizontal = horizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pushapi),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = address.ifBlank { stringResource(R.string.not_configured) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = "Capture Statistics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = CaptureRepository.packets.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.capture_mode_sso),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = CaptureRepository.actions.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = stringResource(R.string.capture_mode_action),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
