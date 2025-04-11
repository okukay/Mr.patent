package com.ssafy.mr_patent_android.ui.study

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.data.model.dto.QuizDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.studyService
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {
    private val _quizData = MutableLiveData<QuizDto>()
    val quizData: LiveData<QuizDto> get() = _quizData

    private val _wrongAnswers = MutableLiveData<ArrayList<Int>>()
    val wrongAnswers: LiveData<ArrayList<Int>> get() = _wrongAnswers

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    fun addWrongQuiz(wordId: Int) {
        _wrongAnswers.value = (_wrongAnswers.value ?: ArrayList()).apply {
            add(wordId)
        }
    }

    fun getQuiz(levelId: Int) {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                studyService.getQuiz(levelId)
            }.onSuccess {
                if (it.isSuccessful) {
                    it.body()?.data?.let { it1 ->
                        _quizData.value = it1
                    }
                } else {
                    _quizData.value = QuizDto()
                }

                _loading.value = false
            }.onFailure {
                _loading.value = false

            }
        }
    }


}
