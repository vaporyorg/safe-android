package io.gnosis.safe.ui.beggar.donate

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.PublishViewModel
import javax.inject.Inject

class SendFundsViewModel
@Inject constructor(appDispatchers: AppDispatchers) :
    PublishViewModel<SendFundsState>(appDispatchers) {
}


data class SendFundsState(override var viewAction: BaseStateViewModel.ViewAction?) : BaseStateViewModel.State
