package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token")val refreshToken: String,
    @SerializedName("user_id")val userId: Int,
    @SerializedName("user_name")val userName: String,
    @SerializedName("user_role")val userRole: Int
)