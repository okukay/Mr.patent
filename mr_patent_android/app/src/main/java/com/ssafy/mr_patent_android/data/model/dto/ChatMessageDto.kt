package com.ssafy.mr_patent_android.data.model.dto

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class ChatMessageDto(
    @SerializedName("status")val status:Int,
    @SerializedName("chatId")val chatId: Int?,
    @SerializedName("read")val isRead: Boolean,
    @SerializedName("message")val message: String?,
    @SerializedName("receiverId")val receiverId: Int?,
    @SerializedName("roomId")val roomId: String,
    @SerializedName("timeStamp")val timestamp: String?,
    @SerializedName("type")val type: String,
    @SerializedName("userId")val userId: Int,
    @SerializedName("fileName")val fileName: String?,
    @SerializedName("fileUrl")val fileUrl: String?,
    @SerializedName("fileUri")val fileUri: Uri?,
    @SerializedName("messageType")val messageType: String
){
    constructor(): this(0,null, false, null, null, "", null, "", 0, "", "", Uri.EMPTY, "")

    constructor(timestamp: String?,messageType: String): this(0,null, false, null, null, "", timestamp, "", 0, "", "", Uri.EMPTY, messageType)

    constructor(chatId: Int?,timestamp: String?,messageType: String): this(0,chatId, false, null, null, "", timestamp, "", 0, "", "", Uri.EMPTY, messageType)

    data class Files(
        @SerializedName("file_name") var fileName: String,
        @SerializedName("file_url") var fileUrl: String,
        @SerializedName("file_uri")val fileUri: Uri
    )

}