package io.gnosis.safe.ui.beggar.donate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentSendFundsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
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
        }
    }
}
