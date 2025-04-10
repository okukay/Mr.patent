package com.ssafy.mr_patent_android.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.base.ErrorResponse
import com.ssafy.mr_patent_android.data.model.dto.FcmRequest
import com.ssafy.mr_patent_android.data.model.dto.LoginRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import kotlinx.coroutines.launch

private const val TAG = "LoginViewModel"
class LoginViewModel:ViewModel() {
    private val _loginState = MutableLiveData<Boolean>()
    val loginState: LiveData<Boolean>
        get() = _loginState

    private val _toast = MutableLiveData<String>()
    val toast: LiveData<String>
        get() = _toast

    fun sendFcmToken(token: String) {
        viewModelScope.launch {
            runCatching {
                authService.sendFcmToken(FcmRequest(token, sharedPreferences.getUser().userId))
            }.onSuccess {
                if (it.isSuccessful) {
                } else {
                    it.errorBody()?.let {
                        networkUtil.getErrorResponse(it)
                    }
                }
            }.onFailure {
                Log.d(TAG, "sendFcmToken: $it")
            }
        }
    }


    fun login(email: String, pwd: String) {
        // 로그인 요청
        viewModelScope.launch {
            runCatching {
                authService.login(LoginRequest(email, pwd))
            }.onSuccess {
                // 로그인 성공
                if (it.isSuccessful) {
                    // 로그인 성공
                    it.body()?.data?.let { res ->
                        sharedPreferences.addAToken(res.accessToken)

                        sharedPreferences.addRToken(res.refreshToken)
                        // 사용자 정보 저장
                       sharedPreferences.addUser(UserDto(res.userId,res.userName,res.userRole))
                        _loginState.value = true
                    }
                } else {
                    it.errorBody()?.let { it1 ->
                        val data = Gson().fromJson(it1.string(), ErrorResponse::class.java)
                        _toast.value = data.error?.message
                    }
                }

            }.onFailure {
                // 로그인 실패
            }
        }
    }


}