package com.ssafy.mr_patent_android.data.model.response

import com.google.gson.annotations.SerializedName

data class ImagePreSignedResponse(
    @SerializedName("url") val url: String
)