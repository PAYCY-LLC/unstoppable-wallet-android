package io.horizontalsystems.bankwallet.modules.btcblockchainsettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class BtcBlockchainSettingsFragment : BaseFragment() {

    private val viewModel by viewModels<BtcBlockchainSettingsViewModel> {
        BtcBlockchainSettingsModule.Factory(requireArguments())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    BtcBlockchainSettingsScreen(
                        viewModel,
                        findNavController()
                    )
                }
            }
        }
    }

}

@Composable
private fun BtcBlockchainSettingsScreen(
    viewModel: BtcBlockchainSettingsViewModel,
    navController: NavController
) {

    if (viewModel.closeScreen) {
        navController.popBackStack()
    }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.PlainString(viewModel.title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))

                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.BtcBlockchainSettings_RestoreSourceChangeWarning)
                )

                RestoreSourceSettings(viewModel, navController)

                TransactionDataSortSettings(viewModel, navController)

                Spacer(Modifier.height(30.dp))
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    title = stringResource(R.string.Button_Save),
                    enabled = viewModel.saveButtonEnabled,
                    onClick = { viewModel.onSaveClick() }
                )
            }
        }

    }
}

@Composable
private fun RestoreSourceSettings(
    viewModel: BtcBlockchainSettingsViewModel,
    navController: NavController
) {
    BlockchainSettingSection(
        viewModel.restoreSources,
        R.id.btcBlockchainRestoreSourceInfoFragment,
        R.string.BtcBlockchainSettings_RestoreSource,
        R.string.BtcBlockchainSettings_RestoreSourceSettingsDescription,
        { viewItem -> viewModel.onSelectRestoreMode(viewItem) },
        navController
    )
}

@Composable
private fun TransactionDataSortSettings(
    viewModel: BtcBlockchainSettingsViewModel,
    navController: NavController
) {
    BlockchainSettingSection(
        restoreSources = viewModel.transactionSortModes,
        infoScreenId = R.id.btcBlockchainTransactionInputOutputsInfoFragment,
        settingTitleTextRes = R.string.BtcBlockchainSettings_TransactionInputsOutputs,
        settingDescriptionTextRes = R.string.BtcBlockchainSettings_TransactionInputsOutputsSettingsDescription,
        onItemClick = { viewItem -> viewModel.onSelectTransactionMode(viewItem) },
        navController = navController
    )
}

@Composable
private fun BlockchainSettingSection(
    restoreSources: List<BtcBlockchainSettingsModule.ViewItem>,
    infoScreenId: Int,
    settingTitleTextRes : Int,
    settingDescriptionTextRes: Int,
    onItemClick: (BtcBlockchainSettingsModule.ViewItem) -> Unit,
    navController: NavController
) {
    Spacer(Modifier.height(12.dp))
    CoinListHeaderWithInfoButton(
      settingTitleTextRes,
    ) {
        navController.slideFromBottom(infoScreenId)
    }
    CellMultilineLawrenceSection(restoreSources) { item ->
        SettingCell(item.title, item.subtitle, item.selected) {
            onItemClick(item)
        }
    }
    Text(
        text = stringResource(settingDescriptionTextRes),
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.grey,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingCell(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = title,
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
    }
}
