package com.ssafy.mr_patent_android.ui.expert

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.userService
import kotlinx.coroutines.launch

private const val TAG = "ExpertListViewModel"

class ExpertListViewModel : ViewModel() {
    private val _expertList = MutableLiveData<List<UserDto>>()
    val expertList: LiveData<List<UserDto>>
        get() = _expertList

    private val _newExpertList = MutableLiveData<List<UserDto>>()
    val newExpertList: LiveData<List<UserDto>>
        get() = _newExpertList

    private val _sortState = MutableLiveData<Int>()
    val sortState: LiveData<Int>
        get() = _sortState

    private val _filterState = MutableLiveData<List<String?>>()
    val filterState: LiveData<List<String?>>
        get() = _filterState

    fun setFilterState(filterState: List<String?>) {
        _filterState.value = filterState
    }

    fun setNewExpertList(list: List<UserDto>?) {
        _newExpertList.value = list ?: listOf()
    }

    fun getExpertList() {
        viewModelScope.launch {
            runCatching {
                userService.getExpertList()
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { list ->
                        list.forEach {
                            it.expertCategory.forEach { category ->
                                it.category.add(category.categoryName)
                            }
                        }
                        _expertList.value = list
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }


}