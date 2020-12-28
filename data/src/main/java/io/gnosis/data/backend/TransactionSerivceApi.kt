package io.gnosis.data.backend

import io.gnosis.data.BuildConfig
import io.gnosis.data.backend.dto.SafeInfoDto
import io.gnosis.data.models.ext.CoreTransactionRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Deprecated("use client gw")
interface TransactionServiceApi {

    @GET("v1/safes/{address}")
    suspend fun getSafeInfo(@Path("address") address: String): SafeInfoDto

    @POST("v1/safes/{address}/transactions/")
    suspend fun submitTransactions(
        @Path("address") address: String,
        @Body data: CoreTransactionRequest
    )


    companion object {
        const val BASE_URL = BuildConfig.TRANSACTION_SERVICE_URL
    }
}
