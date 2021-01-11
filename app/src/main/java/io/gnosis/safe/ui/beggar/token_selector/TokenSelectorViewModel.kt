package io.gnosis.safe.ui.beggar.token_selector

import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.PublishViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import java.math.BigInteger
import javax.inject.Inject

class TokenSelectorViewModel
@Inject constructor(
    private val tokenRepository: TokenRepository,
    private val safeRepository: SafeRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : PublishViewModel<TokenActivityState>(appDispatchers) {

    fun fetchAvailableTokens() {
        safeLaunch {
            runCatching {
                val ownerAddress = safeRepository.getActiveSafe()!!.address
                val userDefaultFiat = settingsHandler.userDefaultFiat
                tokenRepository.loadBalanceOf(ownerAddress, userDefaultFiat).items.map { tokenBalance(it.tokenInfo, it.balance) }
            }.onSuccess {
                updateState { TokenActivityState(ShowAvailableTokens(it)) }
            }.onFailure {
                updateState { TokenActivityState(BaseStateViewModel.ViewAction.ShowError(it)) }
            }
        }
    }
}

data class tokenBalance(val tokenInfo: TokenInfo, val balance: BigInteger)

data class ShowAvailableTokens(val tokens: List<tokenBalance>) : BaseStateViewModel.ViewAction

data class TokenActivityState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
