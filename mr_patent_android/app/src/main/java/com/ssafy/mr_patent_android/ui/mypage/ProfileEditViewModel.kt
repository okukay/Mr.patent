package com.ssafy.mr_patent_android.ui.mypage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.model.kotlin.localDate
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.networkUtil
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.model.dto.ProfileEditRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.fileService
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import com.ssafy.mr_patent_android.util.FileUtil
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ProfileEditViewModel_Mr_Patent"
class ProfileEditViewModel : ViewModel() {
    private val _toastMsg = MutableLiveData<String?>()
    val toastMsg: LiveData<String?>
        get() = _toastMsg

    private val _profileImage = MutableLiveData<String?>()
    val profileImage: LiveData<String?>
        get() = _profileImage

    private val _currentImage = MutableLiveData<String?>()
    val currentImage: LiveData<String?>
        get() = _currentImage

    private val _uploadImage = MutableLiveData<String?>()
    val uploadImage: LiveData<String?>
        get() = _uploadImage

    private val _id = MutableLiveData<Int>()
    val id: LiveData<Int>
        get() = _id

    private val _expertId = MutableLiveData<Int>()
    val expertId: LiveData<Int>
        get() = _expertId

    private val _expertInfo = MutableLiveData<UserDto>()
    val expertInfo: LiveData<UserDto>
        get() = _expertInfo

    private val _memberInfo = MutableLiveData<UserDto>()
    val memberInfo: LiveData<UserDto>
        get() = _memberInfo

    private val _editState = MutableLiveData<Boolean?>()
    val editState: LiveData<Boolean?>
        get() = _editState

    private val _userImagePath = MutableLiveData<String>()
    val userImagePath: LiveData<String>
        get() = _userImagePath

    private val _isExpanded = MutableLiveData<MutableList<Boolean>>()
    val isExpanded: LiveData<MutableList<Boolean>>
        get() = _isExpanded

    /**
     * binding
     */
    private val _editCategory = MutableLiveData<MutableList<UserDto.Category>>()
    val editCategory: LiveData<MutableList<UserDto.Category>>
        get() = _editCategory

    private val _editDescription = MutableLiveData<String>()
    val editDescription: LiveData<String>
        get() = _editDescription

    private val _editAddress1 = MutableLiveData<String>()
    val editAddress1: LiveData<String>
        get() = _editAddress1

    private val _editAddress2 = MutableLiveData<String>()
    val editAddress2: LiveData<String>
        get() = _editAddress2

    private val _editPhone = MutableLiveData<String>()
    val editPhone: LiveData<String>
        get() = _editPhone

    fun setCurrentImage(currentImage: String?) {
        _currentImage.value = currentImage
    }

    fun setProfileImage(profileImage: String?) {
        _profileImage.value = profileImage
    }

    fun clearToastMsg() {
        _toastMsg.value = null
    }

    /**
     * set binding
     */
    fun setEditCategory(editCategory: MutableList<UserDto.Category>) {
        _editCategory.value = editCategory
    }

    fun setEditDescription(editDescription: String) {
        _editDescription.value = editDescription
    }

    fun setEditAddress1(editAddress1: String) {
        _editAddress1.value = editAddress1
    }

    fun setEditAddress2(editAddress2: String) {
        _editAddress2.value = editAddress2
    }

    fun setEditPhone(editPhone: String) {
        _editPhone.value = editPhone
    }

    fun setIsExpanded(isExpanded: MutableList<Boolean>) {
        _isExpanded.value = isExpanded.toMutableList()
    }

    fun getImage(fileName: String) {
        viewModelScope.launch {
            runCatching {
                userService.getImage(fileName)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        if (_profileImage.value != response.url) {
                            _profileImage.value = response.url
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

    fun getMemberInfo() {
        viewModelScope.launch {
            runCatching {
                userService.getMember()
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        Log.d(TAG, "getMemberInfo: ${response}")
                        _memberInfo.value = response
                        _expertId.value = response.expertId
                        if (response.userRole == 1) {
                            _editCategory.value = response.expertCategory
                            _editDescription.value = response.expertDescription
                            if (response.expertAddress.contains("\\")) {
                                _editAddress1.value = response.expertAddress.substringBefore("\\")
                                _editAddress2.value = response.expertAddress.substringAfter("\\")
                            } else {
                                _editAddress1.value = response.expertAddress
                            }
                            _editPhone.value = response.expertPhone
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

    fun editUserInfo(profileEditRequest: ProfileEditRequest) {
        viewModelScope.launch {
            runCatching {
                userService.editUserInfo(profileEditRequest)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { response ->
                        _toastMsg.value = response.message
                        val user = sharedPreferences.getUser()
                        sharedPreferences.addUser(
                            UserDto(
                                user.userId, profileEditRequest.userName ?: user.userName , user.userRole
                            )
                        )
                        if (response.message == "회원정보가 성공적으로 수정되었습니다.") {
                            getMemberInfo()
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
            response = uploadFileS3(context, fileUri, preSignedUrl, fileName, extension, contentType)

            if (response) {

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
                    Log.d(TAG, "uploadFileS3: ${file}")
                    val requestBody = file.asRequestBody(contentType.toMediaType())
                    fileService.uploadFile(preSignedUrl, requestBody)
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