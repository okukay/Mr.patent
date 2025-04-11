package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class QuizRequest (
    @SerializedName("answers")val answers: List<Answer>,
){
    data class Answer(
        @SerializedName("question_id")val question_id: Int,
        @SerializedName("option_id")val option_id: Int
    )
}