package com.ssafy.mr_patent_android.ui.patent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.data.model.dto.PatentDraftDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.patentService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.similiarityTestService
import kotlinx.coroutines.launch

class SimiliarityTestViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String>()
    val toastMsg: LiveData<String>
        get() = _toastMsg

    private val _addState = MutableLiveData<Boolean?>()
    val addState: LiveData<Boolean?>
        get() = _addState

    private val _testState = MutableLiveData<String?>()
    val testState: LiveData<String?>
        get() = _testState

    private val _patentId = MutableLiveData<Int>()
    val patentId: LiveData<Int>
        get() = _patentId

    fun setAddState(addState: Boolean?) {
        _addState.value = addState
    }

    fun setTestState(testState: String?) {
        _testState.value = testState
    }

    fun addDraft(folderId: Int, patentDraftDto: PatentDraftDto) {
        viewModelScope.launch {
            runCatching {
                patentService.addDraft(folderId, patentDraftDto)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data.let { response ->
                        _patentId.value = response?.patentDraftId!!
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

    fun similiaritytest(patentDraftId: Int) {
        viewModelScope.launch {
            runCatching {
                similiarityTestService.similiarityTest(patentDraftId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data.let { response ->
                        _testState.value = "processing"
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