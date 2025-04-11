package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class AnswerDto(
    @SerializedName("answers") val answers: List<WordId>
){
    data class WordId(
        @SerializedName("word_id") val wordId: Int
    )
}