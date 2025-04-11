package com.ssafy.mr_patent_android.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.data.model.dto.PwdChangeRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import kotlinx.coroutines.launch

class EmailVerifyViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String?>()
    val toastMsg: LiveData<String?>
        get() = _toastMsg

    private val _codeState = MutableLiveData<Boolean>()
    val codeState: LiveData<Boolean>
        get() = _codeState

    private val _emailVerifyState = MutableLiveData<Boolean>()
    val emailVerifyState: LiveData<Boolean>
        get() = _emailVerifyState

    fun setToastMsg(toastMsg: String?) {
        _toastMsg.value = toastMsg
    }

    fun setCodeState(state: Boolean) {
        _codeState.value = state
    }

    fun setEmailVerifyState(state: Boolean) {
        _emailVerifyState.value = state
    }

    fun reset() {
        _codeState.value = false
        _emailVerifyState.value = false
    }

    fun emailVerifyForgot(email: String, code: Int) {
        viewModelScope.launch {
            runCatching {
                authService.sendCodePwd(
                    PwdChangeRequest(email, code, null)
                )
            }.onSuccess {
                if (it.isSuccessful) {
                    _emailVerifyState.value = true
                } else {
                    it.errorBody()?.let { it1 ->
                    }
                }
            }.onFailure {
            }
        }
    }

    fun emailVerify(email: String, code: String) {
        viewModelScope.launch {
            runCatching {
                authService.emailVerify(email, code)
            }.onSuccess {
                if (it.isSuccessful) {
                    _toastMsg.value = "이메일 인증 성공"
                    _emailVerifyState.value = true
                } else {
                    it.errorBody()?.let { it1 ->
                        val res = networkUtil.getErrorResponse(it1)
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun sendCode(email: String) {
        viewModelScope.launch {
            runCatching {
                authService.sendCode(email)
            }.onSuccess {
                if (it.isSuccessful) {
                    _codeState.value = true
                    it.body()?.data?.let { response ->
                        _toastMsg.value = response.message
                    }
                } else {
                    it.errorBody()?.let { it1 ->
                        val res = networkUtil.getErrorResponse(it1)
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    fun sendCodeForgot(email: String) {
        viewModelScope.launch {
            runCatching {
                authService.sendForgotCode(PwdChangeRequest(email, null, null))
            }.onSuccess {
                if (it.isSuccessful) {
                    _codeState.value = true
                    it.body()?.data?.let { response ->
                        _toastMsg.value = response.message
                    }
                } else {
                    it.errorBody()?.let { it1 ->
                        val res = networkUtil.getErrorResponse(it1)
                    }
                }
            }.onFailure {
            }
        }
    }
}