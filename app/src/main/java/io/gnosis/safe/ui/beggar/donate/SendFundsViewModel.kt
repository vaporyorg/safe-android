package io.gnosis.safe.ui.beggar.donate

import androidx.annotation.StringRes
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import javax.inject.Inject

class SendFundsViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) :
    BaseStateViewModel<SendFundsState>(appDispatchers) {

    override fun initialState(): SendFundsState = SendFundsState(ViewAction.None)

    fun sendTransaction(amount: String, receiver: Solidity.Address) {
        safeLaunch {
            updateState { SendFundsState(UserMessage(R.string.retrieving_safe_nonce_on_chain)) }
            val activeSafe = safeRepository.getActiveSafe()!!.address
            val nonce = safeRepository.getSafeNonce(activeSafe)
            updateState { SendFundsState(UserMessageWithArgs(R.string.retrieved_safe_nonce_on_chain, listOf(nonce.toString()))) }
            val transactionHash = safeRepository.sendEthTxHash(
                safe = activeSafe, receiver = receiver, value = amount.toBigInteger(), nonce = nonce
            )
            updateState { SendFundsState(UserMessageWithArgs(R.string.retrieved_safe_nonce_on_chain, listOf(transactionHash))) }
        }
    }
}

data class UserMessage(@StringRes val messageId: Int) : BaseStateViewModel.ViewAction
data class UserMessageWithArgs(@StringRes val messageId: Int, val arguments: List<Any>) : BaseStateViewModel.ViewAction
data class SendFundsState(override var viewAction: BaseStateViewModel.ViewAction?) : BaseStateViewModel.State
