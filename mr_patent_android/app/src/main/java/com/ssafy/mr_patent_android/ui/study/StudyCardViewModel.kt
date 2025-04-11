package com.ssafy.mr_patent_android.ui.study

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.mr_patent_android.data.model.dto.AnswerDto
import com.ssafy.mr_patent_android.data.model.dto.WordDto
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.studyService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class StudyCardViewModel : ViewModel() {
    private val _wordList = MutableLiveData<List<WordDto.Word>>()
    val wordList: LiveData<List<WordDto.Word>>
        get() = _wordList

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading


    private val _resultData = MutableLiveData<WordDto>()
    val resultData: LiveData<WordDto> get() = _resultData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _bookmarkState = MutableLiveData<Boolean>()
    val bookmarkState: LiveData<Boolean>
        get() = _bookmarkState


    private val _total = MutableLiveData<Int>()
    val total: LiveData<Int>
        get() = _total


    fun getWordList(levelId: Int) {
        _loading.value = true
        viewModelScope.launch {
            runCatching {
                studyService.getWords(levelId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let {
                        _wordList.value = it.data?.words
                    }
                } else {
                    _wordList.value = List(10)
                    { WordDto.Word(true, 1, "뜻", "단어") }
                    Log.d("StudyCardViewModel", "getWordList: ${response.errorBody()}")
                }
                _loading.value = false
            }.onFailure {
                Log.d("StudyCardViewModel", "getWordList: ${it.message}")
                _loading.value = true
            }
        }
    }

    fun createBookmark(position: Int): Boolean {
        val currentList = _wordList.value?.toMutableList() ?: return false
        val word = currentList[position]

        var result = false

        runBlocking {
            _isLoading.value = true
            val response = if (word.is_bookmarked) {
                studyService.deleteBookmark(word.bookmark_id)
            } else {
                studyService.postBookmark(word.word_id)
            }

            if (response.isSuccessful) {
                val updatedWord = word.copy(
                    is_bookmarked = !word.is_bookmarked,
                    bookmark_id = response.body()?.data?.bookmark_id ?: 0
                )
                currentList[position] = updatedWord
                _wordList.value = currentList
                result = true
            }
            _isLoading.value = false
        }

        return result
    }

    fun getBookmarkWordList(levelId: Int) {
        viewModelScope.launch {
            runCatching {
                studyService.getBookmarkWords(levelId)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.let {
                        _wordList.value = it.data?.words
                        _total.value = it.data?.total
                    }
                } else {
                }
            }.onFailure {
            }
        }
    }

    fun getQuizResult(levelId: Int, wrongList: List<Int>) {
        viewModelScope.launch {
            runCatching {
                val answerDto = AnswerDto(
                    answers = wrongList.map { AnswerDto.WordId(it) }
                )

                studyService.postQuizResult(levelId, answerDto)
            }.onSuccess { response ->
                if (response.isSuccessful) {
                    response.body()?.data?.let { result ->

                        val list = result
                        result.wrong_answers.forEach { it ->
                            list.words = result.wrong_answers.map {
                                WordDto.Word(
                                    is_bookmarked = it.is_bookmarked,
                                    word_id = it.word_id,
                                    word_mean = it.word_mean,
                                    word_name = it.word_name,
                                    bookmark_id = it.bookmark_id
                                )

                            }

                        }

                        _wordList.value = list.words
                        _resultData.value = list
                    }
                }
            }.onFailure { error ->
                error.printStackTrace()
            }
        }
    }
}