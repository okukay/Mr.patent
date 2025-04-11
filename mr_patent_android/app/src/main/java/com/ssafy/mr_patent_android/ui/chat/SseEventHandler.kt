package com.ssafy.mr_patent_android.ui.chat

import android.util.Log
import com.google.gson.Gson
import com.launchdarkly.eventsource.MessageEvent
import com.launchdarkly.eventsource.background.BackgroundEventHandler
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto

private const val TAG = "SseEventHandler"

class SseEventHandler(private val viewModel: ChatListViewModel) : BackgroundEventHandler {

    override fun onOpen() {
        Log.d(TAG, "✅ SSE 연결 열림")
    }

    override fun onClosed() {
        Log.d(TAG, "❌ SSE 연결 닫힘")
    }

    override fun onMessage(event: String, messageEvent: MessageEvent) {
        if (event == "connect") {
            Log.d(TAG, "SSE 연결 확인 성공 메시지: $messageEvent")
        } else if (event == "chat-update") {
            val newMessage = Gson().fromJson(messageEvent.data, ChatRoomDto::class.java)
            Log.d(TAG, "새로운 메시지 수신: ${newMessage}")
            if (!newMessage.roomId.isNullOrEmpty()) {
                viewModel.updateChatList(newMessage)
            }

        }
    }

    override fun onComment(comment: String) {
    }

    override fun onError(t: Throwable?) {
        Log.e(TAG, "❌ SSE 오류 발생", t)
    }
}