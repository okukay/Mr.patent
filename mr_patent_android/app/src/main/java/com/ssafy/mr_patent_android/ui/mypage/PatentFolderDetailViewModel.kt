package com.ssafy.mr_patent_android.ui.mypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.data.model.response.PatentListResponse
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.patentService
import kotlinx.coroutines.launch

class PatentFolderDetailViewModel : ViewModel() {
    private val _folderId = MutableLiveData<Int>()
    val folderId: LiveData<Int>
        get() = _folderId

    private val _patents = MutableLiveData<List<PatentListResponse.PatentSummaryInfo>>()
    val patents: LiveData<List<PatentListResponse.PatentSummaryInfo>>
        get() = _patents

    private val _deleteFlag = MutableLiveData<Boolean>()
    val deleteFlag: LiveData<Boolean>
        get() = _deleteFlag

    fun setFolderId(folderId: Int) {
        _folderId.value = folderId
    }

    // TODO: delete
    fun setPatents(patents: List<PatentListResponse.PatentSummaryInfo>) {
        _patents.value = patents
    }

    fun setDeleteFlag(deleteFlag: Boolean) {
        _deleteFlag.value = deleteFlag
    }

    fun getPatentList(folderId: Int) {
        viewModelScope.launch {
            runCatching{
                patentService.getPatentList(folderId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.let { response ->
                        _patents.value = response.data?.patents
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