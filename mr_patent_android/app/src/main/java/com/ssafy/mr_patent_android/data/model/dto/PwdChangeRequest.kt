package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class PwdChangeRequest(
    @SerializedName("user_email") val email: String?,
    @SerializedName("verification_code")val code: Int?,
    @SerializedName("new_password")val password: String?,
)