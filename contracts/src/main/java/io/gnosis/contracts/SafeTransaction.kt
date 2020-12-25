package io.gnosis.contracts

import pm.gnosis.model.Solidity

data class SafeTransaction(
    val to: Solidity.Address,
    val value: Solidity.UInt256,
    val data: Solidity.Bytes,
    val operation: Solidity.UInt8,
    val safetxgas: Solidity.UInt256,
    val basegas: Solidity.UInt256,
    val gasprice: Solidity.UInt256,
    val gastoken: Solidity.Address,
    val refundreceiver: Solidity.Address,
    val _nonce: Solidity.UInt256
)
