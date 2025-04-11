package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("user_email") val email: String,
    @SerializedName("user_pw")val password: String,
)