package com.ssafy.mr_patent_android.data.remote

import com.ssafy.mr_patent_android.base.BaseResponse
import com.ssafy.mr_patent_android.data.model.dto.ChatMessageDto
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatService {
    @POST("chat/rooms/create")
    suspend fun createChatRoom(
        @Body request: ChatRoomRequest
    ) : Response<BaseResponse<String>>

    @GET("chat/rooms/{userId}")
    suspend fun getChatRoomList(
        @Path("userId") userId: Int
    ) : Response<BaseResponse<List<ChatRoomDto>>>

    @GET("chat/rooms/message/{roomId}")
    suspend fun getChatMessageList(
        @Path("roomId") roomId: String,
        @Query("lastMessageId") lastMessageId: Int?
    ) : Response<BaseResponse<List<ChatMessageDto>>>

    @GET("chat/rooms/message/{roomid}")
    suspend fun getFirstChatMessageList(
        @Path("roomid") roomId: String,
    ) : Response<BaseResponse<List<ChatMessageDto>>>
}