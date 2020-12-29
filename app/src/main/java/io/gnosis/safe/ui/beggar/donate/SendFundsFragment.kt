package io.gnosis.safe.ui.beggar.donate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendFundsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.helpers.AddressInputHelper
import io.gnosis.safe.toError
import io.gnosis.safe.ui.assets.collectibles.details.CollectiblesDetailsFragmentArgs
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.beggar.token_selector.TokenSelectorActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class SendFundsFragment : BaseViewBindingFragment<FragmentSendFundsBinding>() {

    @Inject
    lateinit var viewModel: SendFundsViewModel
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
            backButton.setOnClickListener { findNavController().navigateUp() }
            sendButton.setOnClickListener {
                progress.visible(true)
                viewModel.sendTransaction(
                    amount = tokenAmount.text.toString(),
                    receiver = toAddress.address!!
                )
            }
            toAddress.setOnClickListener {
                addressInputHelper.showDialog()
            }
            if (collectible != null) {
                setupUiForErc721()
            } else {
                setupUiForErc20()
            }
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
        changeTokenButton.visible(false)
        tokenAmountLayout.isEnabled = false
        tokenSymbol.setText(collectible?.name.orEmpty())
        tokenAmount.setText(collectible?.id.orEmpty())
    }

    override fun onStart() {
        super.onStart()
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (val actionView = it.viewAction) {
                is UserMessage -> snackbarNewOrExisting(getString(actionView.messageId))
                is UserMessageWithArgs -> {
                    binding.progress.visible(false)
                    snackbarNewOrExisting(getString(actionView.messageId, actionView.arguments[0] as String))
                }
                is BaseStateViewModel.ViewAction.ShowError -> {
                    binding.progress.visible(false)
                    val error = actionView.error.toError()
                    val message =
                        if (actionView.error is CantTransfer) R.string.error_you_are_not_the_owner else R.string.error_description_send_funds
                    errorSnackbar(requireView(), error.message(requireContext(), message))

                }
            }
        })
    }

    private fun handleError(throwable: Throwable, input: String? = null) {
        Timber.e(throwable)
        with(binding) {
            progress.visible(false)
//            nextButton.isEnabled = false

            val error = throwable.toError()
            toAddress.setError(error.message(requireContext(), R.string.error_description_safe_address), input)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TokenSelectorActivity.TOKEN_INFO_REQUEST_CODE) {
            val tokenInfo: TokenInfo? = data?.getParcelableExtra(TokenSelectorActivity.TOKEN_INFO_PARAM_NAME)
            viewModel.selectedToken = tokenInfo
            binding.tokenSymbol.setText(tokenInfo?.symbol)
        } else {
            addressInputHelper.handleResult(requestCode, resultCode, data)
        }
    }

    private fun updateAddress(address: Solidity.Address) {
        with(binding) {
//            nextButton.isEnabled = false
            toAddress.setNewAddress(address)
        }
//        viewModel.validate(address)
    }

    private var snackbar: Snackbar? = null

    private fun snackbarNewOrExisting(message: String) {
        snackbar?.apply {
            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).text = message
            if (!isShown) show()
        } ?: Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).apply {
            setText(message)
            snackbar = this
            show()
        }
    }
}
