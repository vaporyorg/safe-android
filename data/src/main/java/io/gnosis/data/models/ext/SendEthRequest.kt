package io.gnosis.data.models.ext

import com.squareup.moshi.JsonClass
import pm.gnosis.common.adapters.moshi.DecimalNumber
import pm.gnosis.model.Solidity
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class SendEthRequest(
    val receiver: Solidity.Address,
    val sender: Solidity.Address,
    val value: String,
    val data: String,
    val transactionHash: String,
    val signedTransactionHash: String,
    @DecimalNumber val nonce: BigInteger
)
