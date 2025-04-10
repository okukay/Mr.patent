package com.ssafy.mr_patent_android.data.remote

import com.ssafy.mr_patent_android.base.BaseResponse
import com.ssafy.mr_patent_android.data.model.dto.SimiliarityTestRequest
import com.ssafy.mr_patent_android.data.model.response.FitnessResultResponse
import com.ssafy.mr_patent_android.data.model.response.SimiliarityResultResponse
import com.ssafy.mr_patent_android.data.model.response.SimiliarityTestResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SimiliarityTestService {
    @POST("draft/{patent_draft_id}/similarity-check")
    suspend fun similiarityTest(
        @Path("patent_draft_id") patentDraftId: Int,
        @Body similiarityTestRequest: SimiliarityTestRequest
    ): Response<BaseResponse<SimiliarityTestResponse>>

    @GET("patent/fitness/{user_patent_id}")
    suspend fun getFitnessResult(
        @Path("user_patent_id") userPatentId: Int
    ): Response<BaseResponse<FitnessResultResponse>>

    @GET("patent/similarity/{patent_draft_id}")
    suspend fun getSimiliarityResult(
        @Path("patent_draft_id") patentDraftId: Int
    ): Response<BaseResponse<SimiliarityResultResponse>>

    @GET("draft/{patent_draft_id}/export-pdf")
    suspend fun getPdfFile(
        @Path("patent_draft_id") patentDraftId: Int
    ): Response<ResponseBody>
}