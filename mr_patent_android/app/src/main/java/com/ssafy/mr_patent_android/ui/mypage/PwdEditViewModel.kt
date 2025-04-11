package com.ssafy.mr_patent_android.ui.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.PwdEditRequest
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import kotlinx.coroutines.launch

class PwdEditViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String>()
    val toastMsg: LiveData<String>
        get() = _toastMsg

    fun pwdEdit(currentPwd: String, newPwd: String) {
        viewModelScope.launch {
            runCatching {
                userService.pwdEdit(PwdEditRequest(currentPwd, newPwd))
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _toastMsg.value = response.message
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