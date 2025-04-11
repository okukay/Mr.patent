package com.ssafy.mr_patent_android.data.remote

import com.google.gson.Gson
import com.ssafy.mr_patent_android.base.ErrorResponse
import okhttp3.ResponseBody

object NetworkUtil {
    fun getErrorResponse(errorBody: ResponseBody): ErrorResponse? {
        if (errorBody.string().isEmpty()) {
            return ErrorResponse()
        }
        return Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
    }
}