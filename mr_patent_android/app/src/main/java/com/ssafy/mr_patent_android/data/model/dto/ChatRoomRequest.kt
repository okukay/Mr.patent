package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class ChatRoomRequest(
    @SerializedName("userId") val userId: Int,
    @SerializedName("receiverId")val receiverId: Int,
)