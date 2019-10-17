package com.koso.exohome.exohomelibrarysample.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebSocketResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "response") val response: String,
    @Json(name = "status") val status: String,
    @Json(name = "data") var data: Data? = null
)

data class Data(@Json(name = "expires_in") val expires_in: Int, @Json(name = "token") val token: String)