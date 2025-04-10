package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class QuizDto(
    @SerializedName("questions")val questions: List<Question>
){
    constructor() : this(
        questions = listOf()
    )
}