package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class VerifyResponse(
    @SerializedName("message") val message: String,
    @SerializedName("verified_email") val verified_email: String
)