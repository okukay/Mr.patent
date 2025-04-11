package com.ssafy.mr_patent_android.ui.study

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.data.model.dto.LevelDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.studyService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BookmarkViewModel : ViewModel() {
    private val _bookmarkList = MutableLiveData<LevelDto?>()
    val bookmarkList: LiveData<LevelDto?>
        get() = _bookmarkList

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading


    fun getBookmarkList() {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                studyService.getBookmarkList()
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    _bookmarkList.value = response.body()?.data
                } else {
                }
                delay(100)
                _loading.value = false
            }.onFailure { e ->
                _loading.value = false
            }
        }
    }


}