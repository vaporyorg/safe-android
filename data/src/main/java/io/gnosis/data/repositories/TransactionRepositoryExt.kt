package io.gnosis.data.repositories

import io.gnosis.contracts.GnosisSafe
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.backend.TransactionServiceApi
import io.gnosis.data.models.ext.CoreTransactionRequest
import io.gnosis.data.models.ext.SafeTransaction
import io.gnosis.data.models.ext.SendFundsRequest
import io.gnosis.data.utils.toSignatureString
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger

class TransactionRepositoryExt(
    private val gatewayApi: GatewayApi,
    private val transactionServiceApi: TransactionServiceApi,
    private val ethereumRepository: EthereumRepository
) {
    suspend fun getSafeNonce(safeAddress: Solidity.Address): BigInteger =
        GnosisSafe.Nonce.decode(
            ethereumRepository.request(
                EthCall(transaction = Transaction(address = safeAddress, data = GnosisSafe.Nonce.encode()))
            ).result()!!
        ).param0.value

    suspend fun sendEthTxHash(safe: Solidity.Address, safeTransaction: SafeTransaction): ByteArray =
        with(safeTransaction) {
            GnosisSafe.GetTransactionHash.decode(
                ethereumRepository.request(
                    EthCall(
                        transaction = Transaction(
                            safe,
                            data = GnosisSafe.GetTransactionHash.encode(
                                to,
                                value,
                                data,
                                operation,
                                safetxgas,
                                basegas,
                                gasprice,
                                gastoken,
                                refundreceiver,
                                _nonce
                            )
                        )
                    )
                ).result()!!
            ).param0.byteArray
        }

    suspend fun proposeTransaction(safe: Solidity.Address, sendEthRequest: SendFundsRequest.SendEthRequest) =
        gatewayApi.sendEth(safe.asEthereumAddressChecksumString(), sendEthRequest)

    companion object {
        fun sign(ownerKey: BigInteger, hash: ByteArray): String =
            KeyPair.fromPrivate(ownerKey.toByteArray())
                .sign(hash)
                .toSignatureString()

    }
}
