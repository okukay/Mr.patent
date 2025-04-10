package com.ssafy.mr_patent_android.ui.patent

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.EditFolderRequest
import com.ssafy.mr_patent_android.data.model.dto.FolderDto
import com.ssafy.mr_patent_android.data.model.dto.FolderRequest
import com.ssafy.mr_patent_android.data.model.response.PatentRecentResponse
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.patentService
import com.ssafy.mr_patent_android.util.TimeUtil
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private const val TAG = "PatentViewModel_Mr_Patent"
class PatentViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String?>()
    val toastMsg: LiveData<String?>
        get() = _toastMsg

    private val _draftType = MutableLiveData<String>()
    val draftType: LiveData<String>
        get() = _draftType

    private val _patentsRecent = MutableLiveData<List<PatentRecentResponse.PatentDraft>>()
    val patentsRecent: LiveData<List<PatentRecentResponse.PatentDraft>>
        get() = _patentsRecent

    private val _folders = MutableLiveData<MutableList<FolderDto.Folder>>()
    val folders: LiveData<MutableList<FolderDto.Folder>>
        get() = _folders

    // 미리 작성된 초안의 경우 그 초안을 불러오기 위한 id
    private val _patentDraftId = MutableLiveData<Int>()
    val patentDraftId: LiveData<Int>
        get() = _patentDraftId

    // 폴더 id
    private val _folderId = MutableLiveData<Int>()
    val folderId: LiveData<Int>
        get() = _folderId

    private val _editFlag = MutableLiveData<Boolean>()
    val editFlag: LiveData<Boolean>
        get() = _editFlag

    private val _deleteFlag = MutableLiveData<Boolean>()
    val deleteFlag: LiveData<Boolean>
        get() = _deleteFlag

    fun setToastMsg(toastMsg: String) {
        _toastMsg.value = toastMsg
    }

    fun setDraftType(draftType: String) {
        _draftType.value = draftType
    }

    fun setPatentDraftId(patentDraftId: Int) {
        _patentDraftId.value = patentDraftId
    }

    fun setFolderId(folderId: Int) {
        _folderId.value = folderId
    }

    fun setEditFlag(editFlag: Boolean) {
        _editFlag.value = editFlag
    }

    fun setDeleteFlag(deleteFlag: Boolean) {
        _deleteFlag.value = deleteFlag
    }

    fun getFolderList() {
        viewModelScope.launch {
            runCatching {
                patentService.getFolderList(sharedPreferences.getUser().userId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _folders.value = response.folders.toMutableList()
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

    fun getRecentPatentList() {
        viewModelScope.launch {
            runCatching {
                patentService.getRecentPatentList(sharedPreferences.getUser().userId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        if (!response.patentDrafts.isEmpty()) {
                            _patentsRecent.value = response.patentDrafts
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

    fun addFolder(folderName: String) {
        viewModelScope.launch {
            runCatching {
                patentService.addFolder(FolderRequest(
                    sharedPreferences.getUser().userId,
                    folderName
                ))
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _toastMsg.value = "폴더가 생성되었습니다."
                        getFolderList()
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

    fun editFolder(folderId: Int, folderName: String) {
        viewModelScope.launch {
            runCatching {
                patentService.editFolder(folderId, EditFolderRequest(folderName))
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        getFolderList()
                        _toastMsg.value = "폴더 이름이 변경되었습니다."
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

    fun deleteFolder(folderId: Int) {
        viewModelScope.launch {
            runCatching {
                patentService.deleteFolder(folderId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.let { response ->
                        _toastMsg.value = "폴더가 삭제되었습니다."
                        getFolderList()
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