package io.gnosis.safe.ui.beggar.token_selector

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.data.models.assets.TokenInfo
import io.gnosis.safe.databinding.ItemTokenSelectionBinding
import io.gnosis.safe.utils.loadTokenLogo


typealias OnTokenSelected = (TokenInfo) -> Unit

class TokenSelectorAdapter : RecyclerView.Adapter<TokenSelectorAdapter.TokenViewHolder>() {

    var onTokenSelectionListener: OnTokenSelected? = null

    private var items: MutableList<TokenInfo> = mutableListOf()

    fun setItem(newItems: List<TokenInfo>) {
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

        fun bind(tokenInfo: TokenInfo) {
            with(binding) {
                logo.loadTokenLogo(tokenInfo.logoUri)
                tokenText.text = "${tokenInfo.symbol} - ${tokenInfo.name}"
            }
        }
    }
}
