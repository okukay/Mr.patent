package com.ssafy.mr_patent_android.ui.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto
import com.ssafy.mr_patent_android.data.model.dto.LoginRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.chatService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {
    private val _chatRoomList = MutableLiveData<List<ChatRoomDto>>()
    val chatRoomList: LiveData<List<ChatRoomDto>>
        get() = _chatRoomList

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean>
        get() = _loading

    fun getChatRoomList() {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                chatService.getChatRoomList(sharedPreferences.getUser().userId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { chatRoomList ->
                        _chatRoomList.value = chatRoomList.sortedByDescending { it.lastMessageTime }
                    }
                } else {
                    it.errorBody()?.let { it1 ->
                        networkUtil.getErrorResponse(it1)?.let { errorResponse ->
                        }
                    }
                }
                delay(400)
                _loading.value = false
            }.onFailure {
                it.printStackTrace()
                _loading.value = false
            }
        }
    }

    fun updateChatList(newMessage: ChatRoomDto) {
        val currentList = _chatRoomList.value?.toMutableList() ?: mutableListOf()

        currentList.removeAll { it.roomId == newMessage.roomId }

        val tmpUserId = newMessage.userId
        val tmpreceiverId = newMessage.receiverId
        if (tmpUserId != sharedPreferences.getUser().userId) {
            newMessage.userId = tmpreceiverId
            newMessage.receiverId = tmpUserId
        }



        currentList.add(0, newMessage)
        _chatRoomList.postValue(currentList)
    }


}