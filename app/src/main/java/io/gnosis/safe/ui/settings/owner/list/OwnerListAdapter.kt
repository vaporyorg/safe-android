package io.gnosis.safe.ui.settings.owner.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.R
import io.gnosis.safe.databinding.ItemOwnerLocalBinding
import io.gnosis.safe.utils.shortChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible

class OwnerListAdapter(private val ownerListener: OwnerListener, private val forSigningOnly: Boolean = false) :
    RecyclerView.Adapter<BaseOwnerViewHolder>() {

    private val items = mutableListOf<OwnerViewData>()

    fun updateData(data: List<OwnerViewData>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        // notifyItemRemoved(position) was not sufficient
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseOwnerViewHolder, position: Int) {
        when (holder) {
            is LocalOwnerViewHolder -> {
                val owner = items[position] as OwnerViewData.LocalOwner
                holder.bind(owner, ownerListener, position)
            }
            is LocalOwnerForSigningViewHolder -> {
                val owner = items[position] as OwnerViewData.LocalOwner
                holder.bind(owner, ownerListener, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseOwnerViewHolder {
        return when (OwnerItemViewType.values()[viewType]) {
            OwnerItemViewType.LOCAL -> LocalOwnerViewHolder(
                ItemOwnerLocalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            OwnerItemViewType.LOCAL_FOR_SIGN -> LocalOwnerForSigningViewHolder(
                ItemOwnerLocalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item) {
            is OwnerViewData.LocalOwner -> {
                if (forSigningOnly) {
                    OwnerItemViewType.LOCAL_FOR_SIGN
                } else {
                    OwnerItemViewType.LOCAL
                }
            }
        }.ordinal
    }

    override fun getItemCount() = items.size

    enum class OwnerItemViewType {
        LOCAL,
        LOCAL_FOR_SIGN
    }

    interface OwnerListener {
        fun onOwnerRemove(owner: Solidity.Address, position: Int)
        fun onOwnerEdit(owner: Solidity.Address)
        fun onOwnerClick(owner: Solidity.Address)
    }
}

abstract class BaseOwnerViewHolder(
    viewBinding: ViewBinding
) : RecyclerView.ViewHolder(viewBinding.root)


class LocalOwnerViewHolder(private val viewBinding: ItemOwnerLocalBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(owner: OwnerViewData.LocalOwner, ownerListener: OwnerListAdapter.OwnerListener, position: Int) {
        with(viewBinding) {
            val context = root.context
            blockies.setAddress(owner.address)
            ownerAddress.text = owner.address.shortChecksumString()
            title.text = if (owner.name.isNullOrBlank())
                context.getString(
                    R.string.settings_app_imported_owner_key_default_name,
                    owner.address.shortChecksumString()
                ) else owner.name
            root.setOnClickListener {
                ownerListener.onOwnerClick(owner.address)
            }
        }
    }
}

class LocalOwnerForSigningViewHolder(private val viewBinding: ItemOwnerLocalBinding) : BaseOwnerViewHolder(viewBinding) {

    fun bind(owner: OwnerViewData.LocalOwner, ownerListener: OwnerListAdapter.OwnerListener, position: Int) {
        with(viewBinding) {
            val context = root.context
            blockies.setAddress(owner.address)
            ownerAddress.text = owner.address.shortChecksumString()
            title.text = if (owner.name.isNullOrBlank())
                context.getString(
                    R.string.settings_app_imported_owner_key_default_name,
                    owner.address.shortChecksumString()
                ) else owner.name
            arrow.visible(false)
            root.setOnClickListener {
                ownerListener.onOwnerClick(owner.address)
            }
        }
    }
}
