package io.gnosis.safe.ui.beggar.token_selector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.activity.BaseActivity
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject


class TokenSelectorActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: TokenSelectorViewModel

    override fun screenId(): ScreenId? = null

    private val tokenSelectorAdapter by lazy {
        TokenSelectorAdapter().apply {
            onTokenSelectionListener = { token ->
                val resultIntent = Intent()
                resultIntent.putExtra(TOKEN_INFO_PARAM_NAME, token)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewComponent().inject(this)
        setContentView(R.layout.activity_token_selector)

        findViewById<ImageButton>(R.id.back_button)?.setOnClickListener { finish() }
        findViewById<RecyclerView>(R.id.tokens)?.apply {
            adapter = tokenSelectorAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        findViewById<SwipeRefreshLayout>(R.id.refresh)?.setOnRefreshListener { viewModel.fetchAvailableTokens() }

        viewModel.state().observe(this, Observer {
            when (val viewAction = it.viewAction) {
                is ShowAvailableTokens -> {
                    tokenSelectorAdapter.setItem(viewAction.tokens)
                    hideProgress()
                }
                is BaseStateViewModel.ViewAction.ShowError -> {
                    Timber.e(viewAction.error)
                    val error = viewAction.error.toError()
                    errorSnackbar(findViewById<ViewGroup>(android.R.id.content).rootView, error.message(this))
                    hideProgress()
                }
            }
        })
        viewModel.fetchAvailableTokens()
    }

    private fun hideProgress() {
        findViewById<ProgressBar>(R.id.progress)?.visible(false)
        findViewById<SwipeRefreshLayout>(R.id.refresh)?.isRefreshing = false
    }

    companion object {

        const val TOKEN_INFO_REQUEST_CODE = 1000
        const val TOKEN_INFO_PARAM_NAME = "TOKEN_INFO_PARAM_NAME"

        fun buildIntent(context: Context) = Intent(context, TokenSelectorActivity::class.java)
    }
}
