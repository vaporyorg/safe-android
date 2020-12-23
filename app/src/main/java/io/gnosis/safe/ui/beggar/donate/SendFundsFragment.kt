package io.gnosis.safe.ui.beggar.donate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendFundsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class SendFundsFragment : BaseViewBindingFragment<FragmentSendFundsBinding>() {

    @Inject
    lateinit var viewModel: SendFundsViewModel

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
                    receiver = toAddress.text.toString().asEthereumAddress()!!
                )
            }
        }
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
                    errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_send_funds))

                }
            }
        })
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
