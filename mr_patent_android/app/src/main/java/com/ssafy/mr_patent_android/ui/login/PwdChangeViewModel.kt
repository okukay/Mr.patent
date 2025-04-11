package com.ssafy.mr_patent_android.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.data.model.dto.PwdChangeRequest
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import kotlinx.coroutines.launch

class PwdChangeViewModel : ViewModel() {
    private val _changeState = MutableLiveData<Boolean>()
    val changeState: LiveData<Boolean>
        get() = _changeState


    fun changePwd(pwdChangeRequest: PwdChangeRequest) {
        viewModelScope.launch {
            runCatching {
                authService.changePassword(pwdChangeRequest)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { res ->
                        _changeState.value = true
                    }
                } else {
                    it.errorBody()?.let { it1 ->
                        networkUtil.getErrorResponse(it1)
                    }
                }

            }.onFailure {
            }
        }
    }
}