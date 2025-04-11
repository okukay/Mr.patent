package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class PwdEditRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)