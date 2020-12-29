package io.gnosis.data.models.ext

import io.gnosis.contracts.ERC20Contract
import io.gnosis.contracts.ERC721
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHex
import java.math.BigInteger

data class SafeTransaction(
    val to: Solidity.Address,
    val value: Solidity.UInt256,
    val data: Solidity.Bytes,
    val operation: Solidity.UInt8,
    val safeTxGas: Solidity.UInt256,
    val baseGas: Solidity.UInt256,
    val gasPrice: Solidity.UInt256,
    val gasToken: Solidity.Address,
    val refundReceiver: Solidity.Address,
    val nonce: Solidity.UInt256
) {

    fun toSendEtherRequest(senderOwner: Solidity.Address, signature: String, transactionHash: String): SendFundsRequest.SendEthRequest =
        SendFundsRequest.SendEthRequest(
            receiver = to,
            value = value.value.toString(),
            nonce = nonce.value,
            transactionHash = transactionHash,
            sender = senderOwner,
            signedTransactionHash = signature
        )

    fun toSendErc20Request(senderOwner: Solidity.Address, signature: String, transactionHash: String): SendFundsRequest.SendErc20Request =
        SendFundsRequest.SendErc20Request(
            receiver = to,
            data = data.items.toHex(),
            nonce = nonce.value,
            transactionHash = transactionHash,
            sender = senderOwner,
            signedTransactionHash = signature
        )

    fun toSendErc721Request(senderOwner: Solidity.Address, signature: String, transactionHash: String): SendFundsRequest.SendErc721Request =
        SendFundsRequest.SendErc721Request(
            receiver = to,
            data = data.items.toHex(),
            nonce = nonce.value,
            transactionHash = transactionHash,
            sender = senderOwner,
            signedTransactionHash = signature
        )

    companion object {
        fun buildEthTransfer(receiver: Solidity.Address, value: BigInteger, nonce: BigInteger): SafeTransaction =
            SafeTransaction(
                receiver,
                Solidity.UInt256(value),
                Solidity.Bytes("0x".hexToByteArray()),
                Solidity.UInt8(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.Address(BigInteger.ZERO),
                Solidity.Address(BigInteger.ZERO),
                Solidity.UInt256(nonce)
            )

        fun buildErc20Transfer(receiver: Solidity.Address, tokenAddress: Solidity.Address, amount: BigInteger, nonce: BigInteger): SafeTransaction =
            SafeTransaction(
                tokenAddress,
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.Bytes(
                    ERC20Contract.Transfer.encode(
                        _to = receiver,
                        _value = Solidity.UInt256(amount)
                    ).hexToByteArray()
                ),
                Solidity.UInt8(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.Address(BigInteger.ZERO),
                Solidity.Address(BigInteger.ZERO),
                Solidity.UInt256(nonce)
            )

        fun buildErc721Transfer(
            sender: Solidity.Address,
            receiver: Solidity.Address,
            tokenAddress: Solidity.Address,
            amount: BigInteger,
            nonce: BigInteger
        ): SafeTransaction =
            SafeTransaction(
                tokenAddress,
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.Bytes(
                    ERC721.TransferFrom.encode(
                        from = receiver,
                        to = sender,
                        tokenId = Solidity.UInt256(amount)
                    ).hexToByteArray()
                ),
                Solidity.UInt8(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.UInt256(BigInteger.ZERO),
                Solidity.Address(BigInteger.ZERO),
                Solidity.Address(BigInteger.ZERO),
                Solidity.UInt256(nonce)
            )
    }
}
