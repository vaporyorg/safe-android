package io.gnosis.safe.ui.beggar.token_selector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.ActivityTokenSelectorBinding
import io.gnosis.safe.ui.base.activity.BaseActivity
import javax.inject.Inject

class TokenSelectorActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: TokenSelectorViewModel

    private val binding by lazy { ActivityTokenSelectorBinding.inflate(layoutInflater) }

    override fun screenId(): ScreenId? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewComponent().inject(this)
        with(binding) {
            backButton.setOnClickListener { finish() }
        }
    }

    companion object {

        const val TOKEN_INFO_REQUEST_CODE = 1000
        const val TOKEN_INFO_PARAM_NAME = "TOKEN_INFO_PARAM_NAME"

        fun buildIntent(context: Context) = Intent(context, TokenSelectorActivity::class.java)
    }
}
