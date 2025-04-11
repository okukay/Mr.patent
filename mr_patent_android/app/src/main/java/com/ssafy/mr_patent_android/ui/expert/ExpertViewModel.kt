package com.ssafy.mr_patent_android.ui.expert

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.chatService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import kotlinx.coroutines.launch

class ExpertViewModel : ViewModel() {
    private val _expert = MutableLiveData<UserDto>()
    val expert: LiveData<UserDto>
        get() = _expert

    private val _roomId = MutableLiveData<String?>()
    val roomId: LiveData<String?>
        get() = _roomId


    fun getExpert(id: Int) {
        viewModelScope.launch {
            runCatching {
                userService.getExpert(id)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { expert ->
                        expert.expertCategory.forEach { category ->
                            expert.category.add(category.categoryName)
                        }
                        _expert.value = expert
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun startChat(receiverId: Int) {
        viewModelScope.launch {
            runCatching {
                chatService.createChatRoom(
                    ChatRoomRequest(
                        sharedPreferences.getUser().userId,
                        receiverId
                    )
                )
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.let { response ->
                        _roomId.value = response.data
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }

    }


}