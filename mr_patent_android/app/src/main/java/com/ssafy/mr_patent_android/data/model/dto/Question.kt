package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("correct_option") val correctOption: Int,
    @SerializedName("options") val options: List<Option>,
    @SerializedName("word_id") val wordId: Int,
    @SerializedName("question_text") val questionText: String
) {
    data class Option(
        @SerializedName("option_id") val optionId: Int,
        @SerializedName("option_text") val optionText: String
    )
}
