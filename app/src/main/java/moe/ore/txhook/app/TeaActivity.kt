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
import moe.ore.txhook.helper.toHexString

@ExperimentalSerializationApi
class TeaActivity : EasyActivity() {
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
                TeaScreen(action = action, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeaScreen(action: CaptureAction, onBack: () -> Unit) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            R.string.tab_detail,
            R.string.tab_content,
            R.string.tab_result,
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
                label = "tea_tab",
            ) { selected ->
                when (selected) {
                    0 -> InfoSections(
                        baseTitle = stringResource(R.string.title_base_info),
                        baseItems = listOf(
                            stringResource(R.string.field_encrypt) to (action.type == 0).toString(),
                            stringResource(R.string.field_source_size) to action.buffer.size.toString(),
                            stringResource(R.string.field_result_size) to action.result.size.toString(),
                            stringResource(R.string.field_key_size) to action.key.size.toString(),
                            stringResource(R.string.field_timestamp) to action.time.toString(),
                        ),
                        extraTitle = stringResource(R.string.title_extra_info),
                        extraItems = listOf(
                            stringResource(R.string.field_key) to action.key.toHexString(),
                            stringResource(R.string.field_source_app) to sourceName(action.source),
                        ),
                    )
                    1 -> HexViewerCard(action.buffer)
                    else -> HexViewerCard(action.result)
                }
            }
        }
    }
}
