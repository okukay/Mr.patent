package com.ssafy.mr_patent_android.ui.join

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.R
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.data.model.dto.JoinExpertRequest
import com.ssafy.mr_patent_android.data.model.dto.JoinRequest
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.fileService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import com.ssafy.mr_patent_android.ui.login.LoginActivity
import com.ssafy.mr_patent_android.util.FileUtil
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "JoinViewModel_Mr_Patent"
class JoinViewModel: ViewModel() {
    private val _toastMsg = MutableLiveData<String?>()
    val toastMsg: LiveData<String?>
        get() = _toastMsg

    private val _userRole = MutableLiveData<Int>()
    val userRole: LiveData<Int>
        get() = _userRole

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String>
        get() = _userName

    private val _address = MutableLiveData<String>()
    val address: LiveData<String>
        get() = _address

    private val _file = MutableLiveData<String>()
    val file: LiveData<String>
        get() = _file

    private val _checkDuplEmail = MutableLiveData<Boolean?>()
    val checkDuplEmail: LiveData<Boolean?>
        get() = _checkDuplEmail

    private val _uploadImageState = MutableLiveData<Boolean?>()
    val uploadImageState: LiveData<Boolean?>
        get() = _uploadImageState

    private val _uploadFileState = MutableLiveData<Boolean?>()
    val uploadFileState: LiveData<Boolean?>
        get() = _uploadFileState

    private val _joinState = MutableLiveData<Boolean?>()
    val joinState: LiveData<Boolean?>
        get() = _joinState

    private val _userImage = MutableLiveData<String>()
    val userImage: LiveData<String>
        get() = _userImage

    private val _userImagePath = MutableLiveData<String>()
    val userImagePath: LiveData<String>
        get() = _userImagePath

    private val _userFilePath = MutableLiveData<String>()
    val userFilePath: LiveData<String>
        get() = _userFilePath

    fun setToastMsg(toastMsg: String?) {
        _toastMsg.value = toastMsg
    }

    fun setUserRole(userRole: Int) {
        _userRole.value = userRole
    }

    fun setUserImage(userImage: String) {
        _userImage.value = userImage
    }

    fun setFile(file: String) {
        _file.value = file
    }

    fun setCheckDuplEmail(checkDuplEmail: Boolean?) {
        _checkDuplEmail.value = checkDuplEmail
    }

    fun setUploadImageState(uploadImageState: Boolean) {
        _uploadImageState.value = uploadImageState
    }

    fun setJoinState(joinState: Boolean) {
        _joinState.value = joinState
    }

    fun reset() {
        _userRole.value = -1
        _userImage.value = ""
        _userName.value = ""
        _address.value = ""
        _file.value = ""
        _checkDuplEmail.value = null
        _uploadImageState.value = null
        _uploadFileState.value = null
        _joinState.value = null
        _toastMsg.value = ""
    }

    fun joinMember(userEmail: String, userName: String, userPw: String, userImage: String) {
        viewModelScope.launch {
            runCatching {
                userService.joinMember(
                    JoinRequest(
                        userEmail
                        , userName
                        , userPw
                        , userImage
                        , 0
                    )
                )
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let {
                        _toastMsg.value = it.message
                        _joinState.value = true
                        _userName.value = userName
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


    fun joinExpert(userEmail: String, userPw: String, userName: String, userImage: String
                   , expertIdentification: String, expertDescription: String, expertAddress: String, expertPhone: String
                   ,expertGetDate: String, expertLicense: String, expertCategory: MutableList<JoinExpertRequest.Category>) {
        viewModelScope.launch {
            runCatching {
                userService.joinExpert(
                    JoinExpertRequest(
                        userEmail
                        , userPw
                        , userName
                        , userImage
                        , 1
                        , expertIdentification
                        , expertDescription
                        , expertAddress
                        , expertPhone
                        , expertGetDate
                        , expertLicense
                        , expertCategory
                    )
                )
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _toastMsg.value = "회원가입에 성공했어요!"
                        _joinState.value = true
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

    fun checkDuplEmail(userEmail: String) {
        viewModelScope.launch {
            runCatching {
                userService.checkDuplEmail(userEmail)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        if (response.available) {
                            _toastMsg.value = "사용 가능한 이메일입니다."
                            _checkDuplEmail.value = false
                        } else {
                            _checkDuplEmail.value = true
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

    suspend fun uploadFile(context: Context, fileUri: Uri, fileName: String, extension: String, contentType: String) {
        runCatching {
            var response = false
            var preSignedUrl = ""

            preSignedUrl = getPreSignedUrl(fileName, contentType)
            Log.d(TAG, "uploadFile: 발급 완료")
            response = uploadFileS3(context, fileUri, preSignedUrl, fileName, extension, contentType)
            if (response) {
                if (contentType == "application/pdf") {
                    _uploadFileState.value = true
                } else {
                    _uploadImageState.value = true
                }
            } else {
                throw Exception("업로드 실패")
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    suspend fun getPreSignedUrl(fileName: String, contentType: String) : String {
        return suspendCoroutine { continuation ->
            viewModelScope.launch {
                runCatching {
                    fileService.getImagePreSignedUrl(fileName, contentType)
                }.onSuccess {
                    if (it.isSuccessful) {
                        it.body()?.data?.let { response ->
                            continuation.resume(response.url)
                        }
                    } else {
                        continuation.resumeWithException(Exception("발급 실패"))
                    }
                }.onFailure {
                    it.printStackTrace()
                    continuation.resumeWithException(it)
                }
            }
        }
    }

    suspend fun uploadFileS3(context: Context, fileUri: Uri, preSignedUrl: String, fileName: String, extension: String, contentType: String) : Boolean {
        return suspendCoroutine { continuation ->
            viewModelScope.launch {
                runCatching {
                    val file = FileUtil().getFileFromUri(context, fileUri, fileName, extension)
                    val requstBody = file.asRequestBody(contentType.toMediaType())
                    fileService.uploadFile(preSignedUrl, requstBody)
                }.onSuccess {
                    if (it.isSuccessful) {
                        continuation.resume(true)
                    } else {
                        continuation.resumeWithException(Exception("업로드 실패"))
                    }
                }.onFailure {
                    it.printStackTrace()
                    continuation.resumeWithException(it)
                }
            }
        }
    }
}