package io.horizontalsystems.bankwallet.modules.sendevmtransaction

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountData
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.ethereumkit.core.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.disposables.CompositeDisposable

class SendEvmTransactionViewModel(
        private val service: SendEvmTransactionService,
        private val coinServiceFactory: EvmCoinServiceFactory,
        private val stringProvider: StringProvider
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val sendEnabledLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<String?>()

    val sendingLiveData = MutableLiveData<Unit>()
    val sendSuccessLiveData = MutableLiveData<ByteArray>()
    val sendFailedLiveData = MutableLiveData<String>()

    val viewItemsLiveData = MutableLiveData<List<ViewItem>>()

    init {
        service.stateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.txDataStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }
        service.sendStateObservable.subscribeIO { sync(it) }.let { disposable.add(it) }

        sync(service.state)
        sync(service.txDataState)
        sync(service.sendState)
    }

    fun send() {
        service.send()
    }

    private fun sync(state: SendEvmTransactionService.State) =
            when (state) {
                SendEvmTransactionService.State.Ready -> {
                    sendEnabledLiveData.postValue(true)
                    errorLiveData.postValue(null)
                }
                is SendEvmTransactionService.State.NotReady -> {
                    sendEnabledLiveData.postValue(false)
                    errorLiveData.postValue(state.errors.firstOrNull()?.let { convertError(it) })
                }
            }

    private fun sync(txDataState: SendEvmTransactionService.TxDataState) {
        val viewItems = txDataState.decoration?.let {
            getViewItems(it, txDataState.additionalItems)
        } ?: getFallbackViewItems(txDataState.transactionData)

        viewItemsLiveData.postValue(viewItems)
    }

    private fun sync(sendState: SendEvmTransactionService.SendState) =
            when (sendState) {
                SendEvmTransactionService.SendState.Idle -> Unit
                SendEvmTransactionService.SendState.Sending -> {
                    sendEnabledLiveData.postValue(false)
                    sendingLiveData.postValue(Unit)
                }
                is SendEvmTransactionService.SendState.Sent -> sendSuccessLiveData.postValue(sendState.transactionHash)
                is SendEvmTransactionService.SendState.Failed -> sendFailedLiveData.postValue(convertError(sendState.error))
            }

    private fun getViewItems(decoration: TransactionDecoration, additionalItems: List<SendEvmData.AdditionalItem>): List<ViewItem>? = when (decoration) {
        is TransactionDecoration.Transfer -> getTransferViewItems(decoration, additionalItems)
        is TransactionDecoration.Eip20Transfer -> getEip20TransferViewItems(decoration, additionalItems)
        is TransactionDecoration.Eip20Approve -> getEip20ApproveViewItems(decoration, additionalItems)
        is TransactionDecoration.Swap -> getSwapViewItems(decoration, additionalItems)
        else -> null
    }

    private fun getTransferViewItems(transfer: TransactionDecoration.Transfer, additionalItems: List<SendEvmData.AdditionalItem>): List<ViewItem> {
        val viewItems = mutableListOf<ViewItem>(ViewItem.Amount(coinServiceFactory.baseCoinService.amountData(transfer.value)))
        getDomainViewItem(additionalItems)?.let {
            viewItems.add(it)
        }
        transfer.to?.let { to ->
            viewItems.add(ViewItem.Address(stringProvider.string(R.string.Send_Confirmation_Receiver), value = to.eip55))
        }
        return viewItems
    }

    private fun getEip20TransferViewItems(eip20Transfer: TransactionDecoration.Eip20Transfer, additionalItems: List<SendEvmData.AdditionalItem>): List<ViewItem>? =
            coinServiceFactory.getCoinService(eip20Transfer.contractAddress)?.let { coinService ->
                val viewItems = mutableListOf<ViewItem>(ViewItem.Amount(coinService.amountData(eip20Transfer.value)))
                getDomainViewItem(additionalItems)?.let {
                    viewItems.add(it)
                }
                viewItems.add(ViewItem.Address(stringProvider.string(R.string.Send_Confirmation_Receiver), eip20Transfer.to.eip55))
                viewItems
            }

    private fun getEip20ApproveViewItems(eip20Approve: TransactionDecoration.Eip20Approve, additionalItems: List<SendEvmData.AdditionalItem>): List<ViewItem>? =
            coinServiceFactory.getCoinService(eip20Approve.contractAddress)?.let { coinService ->
                listOf(
                        ViewItem.Amount(coinService.amountData(eip20Approve.value)),
                        ViewItem.Address(stringProvider.string(R.string.Approve_Spender), eip20Approve.spender.eip55)
                )
            }

    private fun getSwapViewItems(swap: TransactionDecoration.Swap, additionalItems: List<SendEvmData.AdditionalItem>): List<ViewItem>? {
        val coinServiceIn = getCoinService(swap.tokenIn) ?: return null
        val coinServiceOut = getCoinService(swap.tokenOut) ?: return null

        val viewItems = mutableListOf<ViewItem>()

        when (val trade = swap.trade) {
            is TransactionDecoration.Swap.Trade.ExactIn -> {
                viewItems.add(ViewItem.Amount(coinServiceIn.amountData(trade.amountIn)))
                viewItems.add(ViewItem.Amount(coinServiceOut.amountData(trade.amountOutMin)))
            }
            is TransactionDecoration.Swap.Trade.ExactOut -> {
                viewItems.add(ViewItem.Amount(coinServiceIn.amountData(trade.amountInMax)))
                viewItems.add(ViewItem.Amount(coinServiceOut.amountData(trade.amountOut)))
            }
        }

        if (swap.to != service.ownAddress) {
            viewItems.add(ViewItem.Address(stringProvider.string(R.string.Send_Confirmation_Receiver), swap.to.eip55))
        }

        return viewItems
    }

    private fun getCoinService(token: TransactionDecoration.Swap.Token) = when (token) {
        TransactionDecoration.Swap.Token.EvmCoin -> coinServiceFactory.baseCoinService
        is TransactionDecoration.Swap.Token.Eip20Coin -> coinServiceFactory.getCoinService(token.address)
    }

    private fun getFallbackViewItems(transactionData: TransactionData) = listOf(
            ViewItem.Amount(coinServiceFactory.baseCoinService.amountData(transactionData.value)),
            ViewItem.Address(stringProvider.string(R.string.Send_Confirmation_Receiver), transactionData.to.eip55),
            ViewItem.Input(stringProvider.string(R.string.Send_Confirmation_Input), transactionData.input.toHexString())
    )

    private fun getDomainViewItem(additionalItems: List<SendEvmData.AdditionalItem>): ViewItem? {
        additionalItems.forEach {
            if (it is SendEvmData.AdditionalItem.Domain) {
                return ViewItem.Value(stringProvider.string(R.string.Send_Confirmation_Domain), it.value)
            }
        }
        return null
    }

    private fun convertError(error: Throwable) =
            when (val convertedError = error.convertedError) {
                is SendEvmTransactionService.TransactionError.InsufficientBalance -> {
                    stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalance, coinServiceFactory.baseCoinService.coinValue(convertedError.requiredBalance))
                }
                is EvmError.InsufficientBalanceWithFee,
                is EvmError.ExecutionReverted -> {
                    stringProvider.string(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, coinServiceFactory.baseCoinService.coin.code)
                }
                else -> convertedError.message ?: convertedError.javaClass.simpleName
            }

}

sealed class ViewItem {
    class Amount(val amountData: AmountData) : ViewItem()
    class Address(val title: String, val value: String) : ViewItem()
    class Input(val title: String, val value: String) : ViewItem()
    class Value(val title: String, val value: String) : ViewItem()
}