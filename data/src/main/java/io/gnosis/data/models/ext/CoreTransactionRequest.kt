package io.gnosis.data.models.ext

import com.squareup.moshi.JsonClass
import pm.gnosis.common.adapters.moshi.DecimalNumber
import pm.gnosis.model.Solidity
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class CoreTransactionRequest(
    val to: Solidity.Address,
    @DecimalNumber val value: BigInteger,
    val data: String,
    @DecimalNumber val nonce: BigInteger,
    @DecimalNumber val operation: BigInteger,
    @DecimalNumber val safeTxGas: BigInteger,
    @DecimalNumber val baseGas: BigInteger,
    @DecimalNumber val gasPrice: BigInteger,
    val gasToken: Solidity.Address,
    val refundReceiver: Solidity.Address,
    val contractTransactionHash: String,
    val sender: Solidity.Address,
    val signature: String
)
