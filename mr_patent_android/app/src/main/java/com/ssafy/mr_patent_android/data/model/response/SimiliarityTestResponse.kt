package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class SimiliarityTestResponse(
    @SerializedName("task_id") val taskId: String
    , @SerializedName("status") val status: String
    , @SerializedName("message") val message: String
)
