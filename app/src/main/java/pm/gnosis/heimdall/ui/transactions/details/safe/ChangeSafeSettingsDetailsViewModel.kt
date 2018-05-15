package pm.gnosis.heimdall.ui.transactions.details.safe

import android.content.Context
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.utils.GnosisSafeUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

class ChangeSafeSettingsDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detailsRepository: TransactionDetailsRepository,
    private val safeRepository: GnosisSafeRepository
) : ChangeSafeSettingsDetailsContract() {

    private var cachedAddOwnerInfo: Pair<String, Int>? = null

    override fun loadFormData(preset: Transaction?): Single<Pair<String, Int>> =
    // Check if we have a cached value
        cachedAddOwnerInfo?.let { Single.just(it) } ?:
        // Else parse preset to extract address
        preset?.let {
            detailsRepository.loadTransactionData(preset)
                .map {
                    val data = it.toNullable()
                    when (data) {
                        is AddSafeOwnerData -> data.let {
                            val address =
                                if (it.newOwner == Solidity.Address(BigInteger.ZERO)) ""
                                else it.newOwner.asEthereumAddressString()
                            address to it.newThreshold
                        }
                        else -> throw IllegalArgumentException()
                    }
                }
                .doOnSuccess {
                    cachedAddOwnerInfo = it
                }
                .onErrorReturnItem(EMPTY_FORM_DATA)
        } ?: Single.just(EMPTY_FORM_DATA)

    override fun inputTransformer(safeAddress: Solidity.Address?) = ObservableTransformer<CharSequence, Result<SafeTransaction>> {
        Observable.combineLatest(
            loadSafeInfo(safeAddress),
            it,
            BiFunction { info: Optional<SafeInfo>, input: CharSequence -> info.toNullable() to input.toString() }
        )
            .map { (safeInfo, input) ->
                try {
                    DataResult(buildTransaction(input, safeAddress, safeInfo))
                } catch (t: Throwable) {
                    ErrorResult<SafeTransaction>(t)
                }
            }
    }

    private fun loadSafeInfo(safeAddress: Solidity.Address?) =
        safeAddress?.let {
            safeRepository.loadInfo(safeAddress)
                .map { it.toOptional() }
                .onErrorReturnItem(None)
        } ?: Observable.just(None)

    private fun buildTransaction(input: String, safe: Solidity.Address?, safeInfo: SafeInfo?): SafeTransaction {
        if (input.isBlank()) {
            throw TransactionInputException(context.getString(R.string.invalid_ethereum_address), TransactionInputException.TARGET_FIELD, false)
        }
        val newOwner = input.asEthereumAddress()
        if (newOwner == null || newOwner == Solidity.Address(BigInteger.ZERO)) {
            throw TransactionInputException(context.getString(R.string.invalid_ethereum_address), TransactionInputException.TARGET_FIELD, true)
        }
        // If we have safe info we should check that the owner does not exist yet
        if (safeInfo?.owners?.contains(newOwner) == true) {
            throw TransactionInputException(context.getString(R.string.error_owner_already_added), TransactionInputException.TARGET_FIELD, true)
        }
        // TODO: add proper error message
        SimpleLocalizedException.assert(safe != null, context, R.string.unknown_error)
        val newThreshold = cachedAddOwnerInfo?.second ?: safeInfo?.owners?.size?.let { GnosisSafeUtils.calculateThreshold(it + 1) }
        SimpleLocalizedException.assert(newThreshold != null, context, R.string.unknown_error)
        val addOwnerData = GnosisSafe.AddOwner.encode(newOwner, Solidity.UInt8(BigInteger.valueOf(newThreshold!!.toLong())))
        // Update cached values
        cachedAddOwnerInfo = input to newThreshold
        return SafeTransaction(Transaction(safe!!, data = addOwnerData), TransactionRepository.Operation.CALL)
    }

    override fun loadAction(safeAddress: Solidity.Address?, transaction: Transaction?): Single<Action> =
        transaction?.let {
            detailsRepository.loadTransactionData(transaction)
                .map {
                    val data = it.toNullable()
                    when (data) {
                        is RemoveSafeOwnerData -> Action.RemoveOwner(data.owner)
                        is AddSafeOwnerData -> Action.AddOwner(data.newOwner)
                        is ReplaceSafeOwnerData -> Action.ReplaceOwner(data.newOwner, data.owner)
                        else -> throw IllegalStateException()
                    }
                }
        } ?: Single.error<Action>(IllegalStateException())

    companion object {
        val EMPTY_FORM_DATA = "" to -1
    }
}
