package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class FcmRequest(
    @SerializedName("fcmToken")val fcmToken: String,
    @SerializedName("userId")val userId: Int
)