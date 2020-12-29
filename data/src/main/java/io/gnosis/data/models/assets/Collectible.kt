package io.gnosis.data.models.assets

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.models.ext.SolidityAddressParceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
@TypeParceler<Solidity.Address, SolidityAddressParceler>
@Parcelize
data class Collectible(
    @Json(name = "id") val id: String,
    @Json(name = "address") val address: Solidity.Address,
    @Json(name = "tokenName") val tokenName: String,
    @Json(name = "tokenSymbol") val tokenSymbol: String,
    @Json(name = "uri") val uri: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "imageUri") val imageUri: String?,
    @Json(name = "logoUri") val logoUri: String?
) : Parcelable
