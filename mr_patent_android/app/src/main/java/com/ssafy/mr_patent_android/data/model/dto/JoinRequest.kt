package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class JoinRequest(
    @SerializedName("user_email") val userEmail: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_pw") val userPw: String,
    @SerializedName("user_image") val userImage: String,
    @SerializedName("user_role") val userRole: Int
)