package io.horizontalsystems.bankwallet.modules.sendx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.fee.HSFeeInput
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.AddressCell
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.ConfirmAmountCell
import io.horizontalsystems.bankwallet.modules.send.submodules.confirmation.SectionTitleCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

class SendConfirmationFragment : BaseFragment() {

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
                val viewModel by navGraphViewModels<SendViewModel>(R.id.sendXFragment)
                val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment)
                val xRateViewModel by navGraphViewModels<XRateViewModel>(R.id.sendXFragment)

                SendConfirmationScreen(
                    findNavController(),
                    viewModel,
                    xRateViewModel,
                    amountInputModeViewModel
                )
            }
        }
    }
}

@Composable
fun SendConfirmationScreen(
    navController: NavController,
    sendViewModel: SendViewModel,
    xRateViewModel: XRateViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val confirmationViewItem = sendViewModel.getConfirmationViewItem()

    ComposeAppTheme {
        Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Send_Confirmation_Title),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack(R.id.sendXFragment, true)
                        }
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 106.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CellSingleLineLawrenceSection2 {
                        CellSingleLineLawrence {
                            SectionTitleCell(
                                R.string.Send_Confirmation_YouSend,
                                confirmationViewItem.platformCoin.name
                            )
                        }
                        CellSingleLineLawrence(borderTop = true) {
                            val coinAmount = App.numberFormatter.formatCoin(
                                confirmationViewItem.amount,
                                confirmationViewItem.platformCoin.coin.code,
                                0,
                                sendViewModel.coinMaxAllowedDecimals
                            )

                            val currencyAmount = xRateViewModel.rate?.let { rate ->
                                rate.copy(value = confirmationViewItem.amount.times(rate.value)).getFormatted(sendViewModel.fiatMaxAllowedDecimals, sendViewModel.fiatMaxAllowedDecimals)
                            }

                            ConfirmAmountCell(currencyAmount, coinAmount, true)
                        }
                        CellSingleLineLawrence(borderTop = true) {
                            AddressCell(confirmationViewItem.address.hex, {  })
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HSFeeInput(
                        coinCode = confirmationViewItem.platformCoin.code,
                        coinDecimal = sendViewModel.coinMaxAllowedDecimals,
                        fiatDecimal = sendViewModel.fiatMaxAllowedDecimals,
                        fee = confirmationViewItem.fee,
                        amountInputType = amountInputModeViewModel.inputType,
                        rate = xRateViewModel.rate
                    )
                }

                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    title = stringResource(R.string.Send_Confirmation_Send_Button),
                    onClick = {
                        sendViewModel.onClickSend()
                    },
                    enabled = true
                )
            }
        }
    }

}


