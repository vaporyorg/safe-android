package pm.gnosis.heimdall.data.repositories.impls.wc

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.modes.CBCBlockCipher
import org.spongycastle.crypto.paddings.PKCS7Padding
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV
import pm.gnosis.heimdall.data.repositories.impls.Session
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import java.security.SecureRandom

class MoshiPayloadAdapter(moshi: Moshi) : Session.PayloadAdapter {

    private val payloadAdapter = moshi.adapter(EncryptedPayload::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
    )

    private fun createRandomBytes(i: Int) = ByteArray(i).also { SecureRandom().nextBytes(it) }

    override fun parse(payload: String, key: String): Session.MethodCall {
        val encryptedPayload = payloadAdapter.fromJson(payload) ?: throw IllegalArgumentException("Invalid json payload!")

        // TODO verify hmac

        val padding = PKCS7Padding()
        val aes = PaddedBufferedBlockCipher(
            CBCBlockCipher(AESEngine()),
            padding
        )
        val ivAndKey = ParametersWithIV(
            KeyParameter(key.hexToByteArray()),
            encryptedPayload.iv.hexToByteArray()
        )
        aes.init(false, ivAndKey)

        val encryptedData = encryptedPayload.data.hexToByteArray()
        val minSize = aes.getOutputSize(encryptedData.size)
        val outBuf = ByteArray(minSize)
        var len = aes.processBytes(encryptedData, 0, encryptedData.size, outBuf, 0)
        len += aes.doFinal(outBuf, len)

        return outBuf.copyOf(len).toMethodCall()
    }

    override fun prepare(data: Session.MethodCall, key: String): String {
        val bytesData = data.toBytes()
        val hexKey = key.hexToByteArray()
        val iv = createRandomBytes(16)

        val padding = PKCS7Padding()
        val aes = PaddedBufferedBlockCipher(
            CBCBlockCipher(AESEngine()),
            padding
        )
        aes.init(true, ParametersWithIV(KeyParameter(hexKey), iv))

        val minSize = aes.getOutputSize(bytesData.size)
        val outBuf = ByteArray(minSize)
        val length1 = aes.processBytes(bytesData, 0, bytesData.size, outBuf, 0)
        aes.doFinal(outBuf, length1)


        val hmac = HMac(SHA256Digest())
        hmac.init(KeyParameter(hexKey))

        val hmacResult = ByteArray(hmac.macSize)
        hmac.update(outBuf, 0, outBuf.size)
        hmac.update(iv, 0, iv.size)
        hmac.doFinal(hmacResult, 0)

        return payloadAdapter.toJson(
            EncryptedPayload(
                outBuf.toHexString(),
                hmac = hmacResult.toHexString(),
                iv = iv.toHexString()
            )
        )
    }

    /**
     * Convert FROM request bytes
     */
    private fun ByteArray.toMethodCall(): Session.MethodCall =
        String(this).let { json ->
            mapAdapter.fromJson(json)?.let {
                try {
                    val method = it["method"]
                    when (method) {
                        "wc_sessionRequest" -> it.toSessionRequest()
                        "wc_sessionUpdate" -> it.toSessionUpdate()
                        "wc_exchangeKey" -> it.toExchangeKey()
                        "eth_sendTransaction" -> it.toSendTransaction()
                        "eth_sign" -> it.toSignMessage()
                        null -> it.toResponse()
                        else -> it.toCustom()
                    }
                } catch (e: Exception) {
                    throw Session.MethodCallException.InvalidRequest(it.getId(), json)
                }
            } ?: throw IllegalArgumentException("Invalid json")
        }

    private fun Map<String, *>.getId(): Long =
        (this["id"] as? Double)?.toLong() ?: throw IllegalArgumentException("id missing")

    private fun Map<String, *>.toSessionRequest(): Session.MethodCall.SessionRequest {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val data = params.firstOrNull() as? Map<*, *> ?: throw IllegalArgumentException("Invalid params")

        return Session.MethodCall.SessionRequest(
            getId(),
            data.extractPeerData()
        )
    }

    private fun Map<String, *>.toSessionUpdate(): Session.MethodCall.SessionUpdate {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val data = params.firstOrNull() as? Map<*, *> ?: throw IllegalArgumentException("Invalid params")
        val approved = data["approved"] as? Boolean ?: throw IllegalArgumentException("approved missing")
        val chainId = data["chainId"] as? Long
        val message = data["message"] as? String
        val accounts = nullOnThrow { (data["accounts"] as? List<*>)?.toStringList() }
        return Session.MethodCall.SessionUpdate(
            getId(),
            Session.SessionParams(approved, chainId, accounts, message)
        )
    }

    private fun Map<String, *>.toExchangeKey(): Session.MethodCall.ExchangeKey {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val data = params.firstOrNull() as? Map<*, *> ?: throw IllegalArgumentException("Invalid params")
        val nextKey = data["nextKey"] as? String ?: throw IllegalArgumentException("next key missing")
        return Session.MethodCall.ExchangeKey(
            getId(),
            nextKey,
            data.extractPeerData()
        )
    }

    private fun Map<String, *>.toSendTransaction(): Session.MethodCall.SendTransaction {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val data = params.firstOrNull() as? Map<*, *> ?: throw IllegalArgumentException("Invalid params")
        val from = data["from"] as? String ?: throw IllegalArgumentException("from key missing")
        val to = data["to"] as? String ?: throw IllegalArgumentException("to key missing")
        val nonce = data["nonce"] as? String ?: throw IllegalArgumentException("nonce key missing")
        val gasPrice = data["gasPrice"] as? String ?: throw IllegalArgumentException("gasPrice key missing")
        val gasLimit = data["gasLimit"] as? String ?: throw IllegalArgumentException("gasLimit key missing")
        val value = data["value"] as? String ?: throw IllegalArgumentException("value key missing")
        val txData = data["data"] as? String ?: throw IllegalArgumentException("data key missing")
        return Session.MethodCall.SendTransaction(getId(), from, to, nonce, gasPrice, gasLimit, value, txData)
    }

    private fun Map<String, *>.toSignMessage(): Session.MethodCall.SignMessage {
        val params = this["params"] as? List<*> ?: throw IllegalArgumentException("params missing")
        val address = params.getOrNull(0) as? String ?: throw IllegalArgumentException("Missing address")
        val message = params.getOrNull(1) as? String ?: throw IllegalArgumentException("Missing message")
        return Session.MethodCall.SignMessage(getId(), address, message)
    }

    private fun Map<String, *>.toCustom(): Session.MethodCall.Custom {
        val method = this["method"] as? String ?: throw IllegalArgumentException("method missing")
        val params = this["params"] as? List<*>
        return Session.MethodCall.Custom(getId(), method, params)
    }

    private fun Map<String, *>.toResponse(): Session.MethodCall.Response {
        val result = this["result"]
        val error = this["error"] as? Map<*, *>
        if (result == null && error == null) throw IllegalArgumentException("no result or error")
        return Session.MethodCall.Response(
            getId(),
            result,
            error?.extractError()
        )
    }

    private fun Map<*, *>.extractError(): Session.Error {
        val code = (this["code"] as? Double)?.toLong()
        val message = this["message"] as? String
        return Session.Error(code ?: 0, message ?: "Unknown error")
    }

    private fun Map<*, *>.extractPeerData(): Session.PeerData {
        val peerId = this["peerId"] as? String ?: throw IllegalArgumentException("peerId missing")
        val peerMeta = this["peerMeta"] as? Map<*, *>
        return Session.PeerData(peerId, peerMeta.extractPeerMeta())
    }

    private fun Map<*, *>?.extractPeerMeta(): Session.PeerMeta {
        val description = this?.get("description") as? String
        val url = this?.get("url") as? String
        val name = this?.get("name") as? String
        val ssl = (this?.get("ssl") as? Boolean) ?: false
        val icons = nullOnThrow { (this?.get("icons") as? List<*>)?.toStringList() }
        return Session.PeerMeta(url, name, description, icons, ssl)
    }

    private fun List<*>.toStringList(): List<String> =
        this.map {
            (it as? String) ?: throw IllegalArgumentException("List contains non-String values")
        }

    /**
     * Convert INTO request bytes
     */
    private fun Session.MethodCall.toBytes() =
        mapAdapter.toJson(
            when (this) {
                is Session.MethodCall.SessionRequest -> this.toMap()
                is Session.MethodCall.ExchangeKey -> this.toMap()
                is Session.MethodCall.Response -> this.toMap()
                is Session.MethodCall.SessionUpdate -> this.toMap()
                is Session.MethodCall.SendTransaction -> this.toMap()
                is Session.MethodCall.SignMessage -> this.toMap()
                is Session.MethodCall.Custom -> this.toMap()
            }
        ).toByteArray()

    private fun Session.MethodCall.SessionRequest.toMap() =
        jsonRpc(id, "wc_sessionRequest", peer.intoMap())

    private fun Session.MethodCall.SessionUpdate.toMap() =
        jsonRpc(id, "wc_sessionUpdate", params.intoMap())

    private fun Session.MethodCall.ExchangeKey.toMap() =
        jsonRpc(
            id, "wc_exchangeKey", peer.intoMap(
                mutableMapOf(
                    "nextKey" to nextKey
                )
            )
        )

    private fun Session.MethodCall.SendTransaction.toMap() =
        jsonRpc(
            id, "eth_sendTransaction", mapOf(
                "from" to from,
                "to" to to,
                "nonce" to nonce,
                "gasPrice" to gasPrice,
                "gasLimit" to gasLimit,
                "value" to value,
                "data" to data
            )
        )

    private fun Session.MethodCall.SignMessage.toMap() =
        jsonRpc(
            id, "eth_sign", address, message
        )

    private fun Session.MethodCall.Custom.toMap() =
        jsonRpcWithList(
            id, method, params ?: emptyList<Any>()
        )

    private fun Session.MethodCall.Response.toMap() =
        mutableMapOf(
            "id" to id,
            "jsonrpc" to "2.0"
        ).apply {
            result?.let { this["result"] = result }
            error?.let { this["error"] = error.intoMap() }
        }

    private fun jsonRpc(id: Long, method: String, vararg params: Any) =
        jsonRpcWithList(id, method, params.asList())

    private fun jsonRpcWithList(id: Long, method: String, params: List<*>) =
        mapOf(
            "id" to id,
            "jsonrpc" to "2.0",
            "method" to method,
            "params" to params
        )

    @JsonClass(generateAdapter = true)
    data class EncryptedPayload(
        @Json(name = "data") val data: String,
        @Json(name = "iv") val iv: String,
        @Json(name = "hmac") val hmac: String
    )
}