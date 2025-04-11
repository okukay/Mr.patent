package com.ssafy.mr_patent_android.data.model.dto

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class ChatHeartBeat(
    @SerializedName("roomId")val roomId: String,
    @SerializedName("type")val type: String = "PING",
    @SerializedName("userId")val userId: Int,
){

}