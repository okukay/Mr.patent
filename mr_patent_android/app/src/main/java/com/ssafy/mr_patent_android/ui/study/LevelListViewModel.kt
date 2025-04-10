package com.ssafy.mr_patent_android.ui.study

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.base.ApplicationClass
import com.ssafy.mr_patent_android.data.model.dto.ChatRoomDto
import com.ssafy.mr_patent_android.data.model.dto.LevelDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.studyService
import kotlinx.coroutines.launch

class LevelListViewModel : ViewModel() {
    private val _levelList = MutableLiveData<LevelDto?>()
    val levelList: LiveData<LevelDto?>
        get() = _levelList


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }


    fun getLevels() {
        _isLoading.value=true
        viewModelScope.launch {
            runCatching {
                studyService.getLevels()
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.data?.let { levelDto ->
                        val updatedLevels = levelDto.levels + LevelDto.Level(
                            best_score = 0,
                            is_accessible = true,
                            is_passed = true,
                            level_id = -1,
                            level_name = "단어장",
                            count = 0
                        )
                        _levelList.value = LevelDto(updatedLevels)
                        _isLoading.value=false
                    }


                } else {
                    _isLoading.value=false
                    Log.e("LevelListViewModel", "getLevels: ${response.errorBody()}")
                }
            }.onFailure {
                _isLoading.value=false
                Log.e("LevelListViewModel", "getLevels: ${it.message}")
            }
        }
    }



}