package moe.ore.txhook.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.EasyActivity
import moe.ore.txhook.R
import moe.ore.txhook.app.model.CaptureAction
import moe.ore.txhook.app.ui.compose.HexViewerCard
import moe.ore.txhook.app.ui.compose.InfoSections
import moe.ore.txhook.app.ui.compose.sourceName
import moe.ore.txhook.app.ui.theme.TxHookTheme

@ExperimentalSerializationApi
class TlvActivity : EasyActivity() {
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

        val action = intent.getParcelableExtra<CaptureAction>("data")
        if (action == null) {
            finish()
            return
        }

        setContent {
            TxHookTheme {
                TlvScreen(action = action, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TlvScreen(action: CaptureAction, onBack: () -> Unit) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            R.string.tab_detail,
            R.string.tab_content,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catching_info)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_ios_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryTabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(stringResource(titleRes)) },
                    )
                }
            }

            AnimatedContent(
                targetState = tabIndex,
                label = "tlv_tab",
            ) { selected ->
                when (selected) {
                    0 -> InfoSections(
                        baseTitle = stringResource(R.string.title_base_info),
                        baseItems = listOf(
                            stringResource(R.string.field_type) to "0x${Integer.toHexString(action.what)}",
                        ),
                        extraTitle = stringResource(R.string.title_extra_info),
                        extraItems = listOf(
                            stringResource(R.string.field_source_app) to sourceName(action.source),
                        ),
                    )
                    else -> HexViewerCard(action.buffer)
                }
            }
        }
    }
}
