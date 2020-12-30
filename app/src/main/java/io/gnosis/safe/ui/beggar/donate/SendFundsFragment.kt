package io.gnosis.safe.ui.beggar.donate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.data.repositories.TokenRepository
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendFundsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.AddressInputHelper
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.beggar.token_selector.TokenSelectorActivity
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.hideSoftKeyboard
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class SendFundsFragment : BaseViewBindingFragment<FragmentSendFundsBinding>() {

    @Inject
    lateinit var viewModel: SendFundsViewModel
    private lateinit var uiState: LiveData<SendFundsState>

    private val navArgs by navArgs<SendFundsFragmentArgs>()
    private val addressInputHelper by lazy {
        AddressInputHelper(this, tracker, ::updateAddress, errorCallback = ::handleError)
    }
    private val collectible by lazy { navArgs.collectible }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSendFundsBinding =
        FragmentSendFundsBinding.inflate(inflater, container, false)

    override fun screenId(): ScreenId? = null

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            baseUiSetup()
            if (collectible != null) {
                viewModel.selectedToken = Asset(collectible)
                setupUiForErc721()
            } else {
                setupUiForErc20()
            }
        }
    }

    private fun FragmentSendFundsBinding.baseUiSetup() {
        backButton.setOnClickListener { findNavController().navigateUp() }
        sendButton.setOnClickListener {
            activity?.hideSoftKeyboard()
            progress.visible(true)
            viewModel.sendTransaction(
                amount = tokenAmount.text?.toString(),
                receiver = toAddress.address
            )
        }
        toAddress.setOnClickListener {
            addressInputHelper.showDialog()
        }
    }

    private fun FragmentSendFundsBinding.setupUiForErc20() {
        changeTokenButton.setOnClickListener {
            startActivityForResult(
                TokenSelectorActivity.buildIntent(requireContext()),
                TokenSelectorActivity.TOKEN_INFO_REQUEST_CODE
            )
        }
    }

    private fun FragmentSendFundsBinding.setupUiForErc721() {
        logMultilineMessage(
            "Collectible selected: ",
            "- Name: ${collectible?.name}",
            "- ID: ${collectible?.id}",
            "- Address: ${collectible?.address?.asEthereumAddressChecksumString()}"
        )
        changeTokenButton.visible(false)
        tokenAmountLayout.isEnabled = false
        tokenSymbol.setText(collectible?.name.orEmpty())
        tokenAmount.setText(collectible?.id.orEmpty())
    }

    override fun onStop() {
        super.onStop()
        activity?.hideSoftKeyboard()
        uiState.removeObservers(viewLifecycleOwner)
    }

    override fun onStart() {
        super.onStart()
        uiState = viewModel.state()
        uiState.observe(viewLifecycleOwner, Observer {
            when (val actionView = it.viewAction) {
                is UserMessage -> logMessage(getString(actionView.messageId))
                is UserMessageWithArgs -> {
                    binding.progress.visible(false)
                    logMessage(getString(actionView.messageId, actionView.arguments[0] as String))
                }
                is BaseStateViewModel.ViewAction.ShowError -> {
                    binding.progress.visible(false)
                    val message =
                        when (actionView.error) {
                            is CantTransfer -> R.string.error_you_are_not_the_owner
                            is InvalidAmount -> R.string.error_invalid_amount_or_id
                            is NullReceiver -> R.string.error_must_have_a_receiver
                            else -> R.string.error_description_send_funds
                        }
                    logMessage("⚠️ An error has occurred...${getString(message)}")
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TokenSelectorActivity.TOKEN_INFO_REQUEST_CODE) {
            val tokenInfo: TokenInfo? = data?.getParcelableExtra(TokenSelectorActivity.TOKEN_INFO_PARAM_NAME)
            viewModel.selectedToken = Asset(tokenInfo)
            binding.tokenSymbol.setText(tokenInfo?.symbol)
            logMultilineMessage(
                "Token selected: ",
                "- Symbol: ${tokenInfo?.symbol ?: TokenRepository.NATIVE_CURRENCY_INFO.symbol}",
                "- Name: ${tokenInfo?.name ?: TokenRepository.NATIVE_CURRENCY_INFO.name}",
                "- Address: ${tokenInfo?.address?.asEthereumAddressChecksumString() ?: TokenRepository.NATIVE_CURRENCY_INFO.address.asEthereumAddressChecksumString()}"
            )
        } else {
            addressInputHelper.handleResult(requestCode, resultCode, data)
        }
    }

    private fun handleError(throwable: Throwable, input: String? = null) {
        Timber.e(throwable)
        with(binding) {
            progress.visible(false)
            val error = throwable.toError()
            toAddress.setError(error.message(requireContext(), R.string.error_description_safe_address), input)
        }
    }

    private fun updateAddress(address: Solidity.Address) {
        binding.toAddress.setNewAddress(address)
        logMessage("Updated receiver address: ${address.asEthereumAddressChecksumString()}")
    }

    private fun logMultilineMessage(vararg messages: String) {
        logMessage(messages.first())
        messages.takeLast(messages.size - 1).forEach { logMessage(" $it") }
    }

    private fun logMessage(message: String) {
        with(binding) {
            logText.append("\n> $message")
            logScrollContainer.post {
                logScrollContainer.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}
