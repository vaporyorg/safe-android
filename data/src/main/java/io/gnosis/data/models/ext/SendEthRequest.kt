package io.gnosis.data.models.ext

import android.os.Parcel
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parceler
import pm.gnosis.common.adapters.moshi.DecimalNumber
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import java.math.BigInteger

sealed class SendFundsRequest(val type: SendFundRequestType) {

    @JsonClass(generateAdapter = true)
    data class SendEthRequest(
        val receiver: Solidity.Address,
        val sender: Solidity.Address,
        val value: String,
        val transactionHash: String,
        val signedTransactionHash: String,
        @DecimalNumber val nonce: BigInteger
    ) : SendFundsRequest(SendFundRequestType.Ether)

    @JsonClass(generateAdapter = true)
    data class SendErc20Request(
        val receiver: Solidity.Address,
        val sender: Solidity.Address,
        val data: String,
        val transactionHash: String,
        val signedTransactionHash: String,
        @DecimalNumber val nonce: BigInteger
    ) : SendFundsRequest(SendFundRequestType.Erc20)
}

enum class SendFundRequestType {
    Ether, Erc20
}

object SolidityAddressParceler : Parceler<Solidity.Address> {
    override fun create(parcel: Parcel) = Solidity.Address(parcel.readString()!!.hexAsBigInteger())

    override fun Solidity.Address.write(parcel: Parcel, flags: Int) {
        parcel.writeString(value.toHexString())
    }
}
