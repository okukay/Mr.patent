package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class EmailCodeRequest(
    @SerializedName("user_email") val email: String,
    @SerializedName("verification_code")val code: String,
)