package com.ssafy.mr_patent_android

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto
import com.ssafy.mr_patent_android.data.model.dto.FcmRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"
class MainViewModel: ViewModel() {
    private val _networkState = MutableLiveData<Boolean>()
    val networkState: LiveData<Boolean>
        get() = _networkState

    fun setNetworkState(state: Boolean) {
        _networkState.postValue(state)
    }

    fun getMemberInfo() {
        viewModelScope.launch {
            runCatching {
                userService.getMember()
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        response.userImage?.let {
                            getImage(it)
                        }
                    }
                } else {
                    it.errorBody()?.let {
                            it1 -> networkUtil.getErrorResponse(it1)
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun getImage(fileName: String) {
        viewModelScope.launch {
            runCatching {
                userService.getImage(fileName)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        sharedPreferences.addUserImage(response.url ?: "")
                    }
                } else {
                    it.errorBody()?.let {
                            it1 -> networkUtil.getErrorResponse(it1)
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

}