package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class ChatRoomDto(
    @SerializedName("userId") var userId: Int,
    @SerializedName("expertId") val expertId: Int=-1,
    @SerializedName("userName") val userName: String,
    @SerializedName("userImage") val userImage: String="",
    @SerializedName("roomId")val roomId: String,
    @SerializedName("lastMessage") val lastMessage: String,
    @SerializedName("lastTimestamp")val lastMessageTime: String,
    @SerializedName("unreadCount")val unreadCount: Int,
    @SerializedName("receiverId") var receiverId: Int,
    @SerializedName("userImageName")val userImageName: String="",
){
    constructor() : this(-1,-1,"","","","","",0,-1)
}