package com.ssafy.mr_patent_android.data.model.dto

import com.google.gson.annotations.SerializedName

data class SimiliarityTestRequest(
    @SerializedName("user_token") val userToken: String
)
