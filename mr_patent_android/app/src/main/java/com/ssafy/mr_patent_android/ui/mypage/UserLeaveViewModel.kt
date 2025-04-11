package com.ssafy.mr_patent_android.ui.mypage

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.LogoutRequest
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import kotlinx.coroutines.launch

private const val TAG = "UserLeaveViewModel_Mr_Patent"
class UserLeaveViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String>()
    val toastMsg: LiveData<String>
        get() = _toastMsg

    fun deleteUser() {
        viewModelScope.launch {
            runCatching {
                userService.deleteUser()
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _toastMsg.value = "탈퇴가 완료되었습니다."
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

    fun logout() {
        viewModelScope.launch {
            runCatching {
                authService.logout(
                    LogoutRequest(sharedPreferences.getRToken().toString())
                )
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _toastMsg.value = "로그아웃 되었습니다."
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

    fun clearToken() {
        viewModelScope.launch {
            try {
                authService.deleteFcmToken(sharedPreferences.getUser().userId,)

            }
            catch (e: Exception) {
            }finally {
                logout()
            }
        }
    }

}