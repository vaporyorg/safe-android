package io.gnosis.data.repositories

import io.gnosis.contracts.GnosisSafe
import io.gnosis.data.backend.GatewayApi
import io.gnosis.data.models.Page
import io.gnosis.data.models.core.SafeTransaction
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger

class TransactionRepositoryExt(
    private val gatewayApi: GatewayApi,
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



}

fun <T> Page<T>.adjustLinks(): Page<T> =
    with(this) {
        Page(count,
            next?.replace("127.0.0.1", "10.0.2.2"),
            previous?.replace("127.0.0.1", "10.0.2.2"),
            results)
    }
