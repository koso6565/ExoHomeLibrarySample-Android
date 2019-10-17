package com.koso.exohome.exohomelibrarysample.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionResponse(@Json(name = "id") val id: String, @Json(name = "token") val token: String)