package com.ssafy.mr_patent_android.base

import com.google.gson.annotations.SerializedName

class ErrorResponse(
    @SerializedName("data") val error: Error? = null
){
    data class Error(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String
    )

    constructor() : this(
        error = Error(
            code = 0,
            message = "다시 한번 시도해주세요"
        )
    )
}
