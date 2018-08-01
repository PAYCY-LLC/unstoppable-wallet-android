package bitcoin.wallet.modules.wallet

import bitcoin.wallet.core.DatabaseChangeset
import bitcoin.wallet.core.IDatabaseManager
import bitcoin.wallet.entities.*
import bitcoin.wallet.entities.coins.bitcoin.Bitcoin
import bitcoin.wallet.entities.coins.bitcoin.BitcoinUnspentOutput
import bitcoin.wallet.entities.coins.bitcoinCash.BitcoinCashUnspentOutput
import bitcoin.wallet.modules.RxBaseTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atMost
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class WalletInteractorTest {

    private val delegate = mock(WalletModule.IInteractorDelegate::class.java)
    private val databaseManager = mock(IDatabaseManager::class.java)

    private var exchangeRates = DatabaseChangeset(listOf(
            ExchangeRate().apply {
                code = "BTC"
                value = 10_000.0
            }
    ))

    private val blockchainInfosSyncing = listOf(
            BlockchainInfo().apply {
                coinCode = "BTC"
                latestBlockHeight = 130
                syncing = true

            })

    private val blockchainInfosNotSyncing = listOf(
            BlockchainInfo().apply {
                coinCode = "BTC"
                latestBlockHeight = 135
                syncing = false

            })

    private lateinit var interactor: WalletInteractor

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor = WalletInteractor(databaseManager)
        interactor.delegate = delegate
    }

    @Test
    fun fetchWalletBalances() {
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(exchangeRates))
        whenever(databaseManager.getBalances()).thenReturn(Observable.just(DatabaseChangeset(listOf(Balance().apply {
            code = "BTC"
            value = 80_000_000
        }))))
        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.just(DatabaseChangeset(blockchainInfosNotSyncing)))

        val expectedWalletBalances = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 0.8), 10_000.0, DollarCurrency(), false)
        )

        interactor.notifyWalletBalances()

        verify(delegate, atMost(2)).didFetchWalletBalances(expectedWalletBalances)
    }

    @Test
    fun fetchWalletBalancesWhileSyncing() {
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(exchangeRates))
        whenever(databaseManager.getBalances()).thenReturn(Observable.just(DatabaseChangeset(listOf(Balance().apply {
            code = "BTC"
            value = 80_000_000
        }))))

        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.just(DatabaseChangeset(blockchainInfosSyncing)))

        val expectedWalletBalances = listOf(
                WalletBalanceItem(CoinValue(Bitcoin(), 0.8), 10_000.0, DollarCurrency(), true)
        )

        interactor.notifyWalletBalances()

        verify(delegate, atMost(1)).didFetchWalletBalances(expectedWalletBalances)
    }

    @Test
    fun fetchWalletBalances_emptyRates() {
        whenever(databaseManager.getExchangeRates()).thenReturn(Observable.just(DatabaseChangeset(listOf())))
        whenever(databaseManager.getBalances()).thenReturn(Observable.just(DatabaseChangeset(listOf())))
        whenever(databaseManager.getBlockchainInfos()).thenReturn(Observable.just(DatabaseChangeset(listOf())))
        interactor.notifyWalletBalances()

        verify(delegate, never()).didFetchWalletBalances(any())
    }

}
