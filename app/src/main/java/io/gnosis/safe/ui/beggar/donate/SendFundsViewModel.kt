package io.gnosis.safe.ui.beggar.donate

import androidx.annotation.StringRes
import io.gnosis.data.models.assets.Collectible
import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.data.models.assets.TokenType
import io.gnosis.data.models.ext.SafeTransaction
import io.gnosis.data.models.ext.SendFundsRequest
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepositoryExt
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.utils.OwnerCredentialsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.utils.BigIntegerUtils
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.toHex
import java.math.BigInteger
import javax.inject.Inject

class SendFundsViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val transactionRepositoryExt: TransactionRepositoryExt,
    private val ownerCredentialsRepository: OwnerCredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendFundsState>(appDispatchers) {

    var selectedToken: Asset<*>? = null

    override fun initialState(): SendFundsState = SendFundsState(ViewAction.None)

    fun sendTransaction(amount: String, receiver: Solidity.Address) {
        safeLaunch {
            val activeSafe = safeRepository.getActiveSafe()!!.address
            verifyOwner(activeSafe)

            val nonce = fetchCurrentSafeNonce(activeSafe)

            val safeTransaction = buildSafeTransaction(sender = activeSafe, receiver = receiver, amount = amount.toBigInteger(), nonce = nonce)
            val transactionHash = getTransactionHash(activeSafe, safeTransaction)

            val privateKey = ownerCredentialsRepository.retrieveCredentials() ?: throw NullOwnerKey
            val signature = TransactionRepositoryExt.sign(privateKey.key, transactionHash)

            val sendEthRequest = safeTransaction.buildSendFundsTransfer(
                senderOwner = privateKey.address, transactionHash = transactionHash.toHex().addHexPrefix(), signature = signature.addHexPrefix()
            )
//            runCatching { transactionRepositoryExt.proposeTransaction(activeSafe, sendEthRequest) }
//                .onSuccess { updateState { SendFundsState(UserMessage(R.string.transaction_proposed_successfully)) } }
//                .onFailure { updateState { SendFundsState(ViewAction.ShowError(it)) } }
        }
    }

    private suspend fun getTransactionHash(
        activeSafe: Solidity.Address,
        safeTransaction: SafeTransaction
    ): ByteArray {
        return transactionRepositoryExt.getTransactionHash(safe = activeSafe, safeTransaction = safeTransaction).also { transactionHash ->
            updateState {
                SendFundsState(
                    UserMessageWithArgs(
                        R.string.retrieved_transaction_hash_on_chain,
                        listOf(transactionHash.toHex().addHexPrefix())
                    )
                )
            }
        }
    }

    private suspend fun fetchCurrentSafeNonce(activeSafe: Solidity.Address): BigInteger {
        updateState { SendFundsState(UserMessage(R.string.retrieving_safe_nonce_on_chain)) }
        return transactionRepositoryExt.getSafeNonce(activeSafe).also { nonce ->
            updateState { SendFundsState(UserMessageWithArgs(R.string.retrieved_safe_nonce_on_chain, listOf(nonce.toString()))) }
        }
    }

    private fun buildSafeTransaction(sender: Solidity.Address, receiver: Solidity.Address, amount: BigInteger, nonce: BigInteger): SafeTransaction =
        when (val item = selectedToken?.item) {
            is TokenInfo -> SafeTransaction.buildErc20Transfer(
                receiver = receiver,
                tokenAddress = item.address,
                amount = amount.multiply(BigInteger.TEN.pow(item.decimals)),
                nonce = nonce
            )
            is Collectible -> SafeTransaction.buildErc721Transfer(sender, receiver, item.address, item.id.toBigInteger(), nonce)
            else -> SafeTransaction.buildEthTransfer(receiver = receiver, value = amount, nonce = nonce)
        }

    private fun SafeTransaction.buildSendFundsTransfer(senderOwner: Solidity.Address, transactionHash: String, signature: String): SendFundsRequest =
        when (selectedToken?.item) {
            is TokenInfo -> this.toSendErc20Request(senderOwner = senderOwner, transactionHash = transactionHash, signature = signature)
            is Collectible -> this.toSendErc721Request(senderOwner = senderOwner, transactionHash = transactionHash, signature = signature)
            else -> this.toSendEtherRequest(senderOwner = senderOwner, transactionHash = transactionHash, signature = signature)
        }

    private suspend fun verifyOwner(safe: Solidity.Address) {
        val safeInfo = safeRepository.getSafeInfo(safe)
        takeUnless { ownerCredentialsRepository.hasCredentials() && safeInfo.owners.contains(ownerCredentialsRepository.retrieveCredentials()?.address) }
            ?.run { throw CantTransfer }
    }
}

data class Asset<T>(val item: T)

object CantTransfer : Throwable()
object NullOwnerKey : Throwable()

data class UserMessage(@StringRes val messageId: Int) : BaseStateViewModel.ViewAction
data class UserMessageWithArgs(@StringRes val messageId: Int, val arguments: List<Any>) : BaseStateViewModel.ViewAction
data class SendFundsState(override var viewAction: BaseStateViewModel.ViewAction?) : BaseStateViewModel.State
