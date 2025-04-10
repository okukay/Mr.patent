package com.ssafy.mr_patent_android.ui.mypage

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.data.model.dto.PatentFrameDto
import com.ssafy.mr_patent_android.data.model.response.FitnessResultResponse
import com.ssafy.mr_patent_android.data.model.response.PatentContentResponse
import com.ssafy.mr_patent_android.data.model.response.PatentRecentResponse
import com.ssafy.mr_patent_android.data.model.response.SimiliarityResultResponse
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.patentService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.similiarityTestService
import kotlinx.coroutines.launch
import java.io.File

class ReportResultViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String>()
    val toastMsg: LiveData<String>
        get() = _toastMsg

    private val _patentContent = MutableLiveData<PatentRecentResponse.PatentDraft>()
    val patentContent: LiveData<PatentRecentResponse.PatentDraft>
        get() = _patentContent

    private val _similiarityResult = MutableLiveData<List<SimiliarityResultResponse.Comparison>>()
    val similiarityResult: LiveData<List<SimiliarityResultResponse.Comparison>>
        get() = _similiarityResult

    private val _mode = MutableLiveData<String>()
    val mode: LiveData<String>
        get() = _mode

    // 초안 기준들
    private val _patentExpContents = MutableLiveData<List<PatentFrameDto>>()
    val patentExpContents: LiveData<List<PatentFrameDto>>
        get() = _patentExpContents

    private val _patentClaimContents = MutableLiveData<List<PatentFrameDto>>()
    val patentClaimContents: LiveData<List<PatentFrameDto>>
        get() = _patentClaimContents

    private val _patentSummaryContents = MutableLiveData<List<PatentFrameDto>>()
    val patentSummaryContents: LiveData<List<PatentFrameDto>>
        get() = _patentSummaryContents

    // 적합도
    private val _fitnessContents = MutableLiveData<FitnessResultResponse.FitnessContent>()
    val fitnessContents: LiveData<FitnessResultResponse.FitnessContent>
        get() = _fitnessContents

    private val _fitnessResult = MutableLiveData<String>()
    val fitnessResult: LiveData<String>
        get() = _fitnessResult

    fun setMode(mode: String) {
        _mode.value = mode
    }

    fun setPatentContent(patentContent: PatentRecentResponse.PatentDraft) {
        _patentContent.value = patentContent
    }

    fun setPatentExpContents(patentExpContents: List<PatentFrameDto>) {
        _patentExpContents.value = patentExpContents
    }

    fun setPatentClaimContents(patentClaimContents: List<PatentFrameDto>) {
        _patentClaimContents.value = patentClaimContents
    }

    fun setPatentSummaryContents(patentSummaryContents: List<PatentFrameDto>) {
        _patentSummaryContents.value = patentSummaryContents
    }

    fun getFitnessResult(userPatentId: Int) {
        viewModelScope.launch {
            runCatching {
                similiarityTestService.getFitnessResult(userPatentId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        if (response.isCorrected == 1) {
                            _fitnessResult.value = "PASS"
                        } else {
                            _fitnessResult.value = "FAIL"
                            _fitnessContents.value = response.details
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

    fun getSimiliarityResult(userPatentId: Int) {
        viewModelScope.launch {
            runCatching {
                similiarityTestService.getSimiliarityResult(userPatentId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body().let { response ->
                        if (response?.data?.comparisons?.isNotEmpty() == true) {
                            _similiarityResult.value = response.data.comparisons
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

    fun getPdfFile(context: Context, patentDraftId: Int, patentDraftTitle: String) {
        viewModelScope.launch {
            runCatching {
                similiarityTestService.getPdfFile(patentDraftId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.let { response ->
                        val inputStream = response.byteStream()
                        val file = File(context.filesDir, "${patentDraftTitle}.pdf")
                        file.outputStream().use { output ->
                            inputStream.copyTo(output)
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

    fun getPatentContent(userPatentId: Int) {
        viewModelScope.launch {
            runCatching {
                patentService.getPatentContent(userPatentId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _patentContent.value = response
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