package io.gnosis.safe.ui.beggar.token_selector

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.databinding.ItemTokenSelectionBinding
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.convertAmount
import io.gnosis.safe.utils.loadTokenLogo
import java.math.BigInteger

typealias OnTokenSelected = (tokenBalance) -> Unit

class TokenSelectorAdapter(private val balanceFormatter: BalanceFormatter) : RecyclerView.Adapter<TokenSelectorAdapter.TokenViewHolder>() {

    var onTokenSelectionListener: OnTokenSelected? = null

    private var items: MutableList<tokenBalance> = mutableListOf()

    fun setItem(newItems: List<tokenBalance>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder =
        TokenViewHolder(ItemTokenSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TokenViewHolder(private val binding: ItemTokenSelectionBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { onTokenSelectionListener?.invoke(items[absoluteAdapterPosition]) }
        }

        fun bind(tokenBalance: tokenBalance) {
            with(binding) {
                logo.loadTokenLogo(tokenBalance.tokenInfo.logoUri)
                tokenText.text = "${tokenBalance.tokenInfo.symbol} - ${tokenBalance.tokenInfo.name}"
                val balance = balanceFormatter.shortAmount(tokenBalance.balance.convertAmount(tokenBalance.tokenInfo.decimals))
                this.tokenBalance.text = "Current balance: $balance"
            }
        }
    }
}
