package com.ssafy.mr_patent_android.data.remote

import com.ssafy.mr_patent_android.base.BaseResponse
import com.ssafy.mr_patent_android.data.model.dto.JoinExpertRequest
import com.ssafy.mr_patent_android.data.model.dto.JoinRequest
import com.ssafy.mr_patent_android.data.model.dto.ProfileEditRequest
import com.ssafy.mr_patent_android.data.model.response.LoginResponse
import com.ssafy.mr_patent_android.data.model.dto.PwdEditRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.model.response.CheckDuplResponse
import com.ssafy.mr_patent_android.data.model.response.MessageResponse
import com.ssafy.mr_patent_android.data.model.response.ProfileEditResponse
import com.ssafy.mr_patent_android.data.model.response.UrlResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {
    @GET("expert")
    suspend fun getExpertList(): Response<BaseResponse<List<UserDto>>>

    @GET("expert/{expertId}")
    suspend fun getExpert(
        @Path("expertId") expertId: Int
    ): Response<BaseResponse<UserDto>>

    @POST("user")
    suspend fun joinMember(
        @Body joinRequest: JoinRequest
    ): Response<BaseResponse<MessageResponse>>

    @GET("user/me")
    suspend fun getMember(): Response<BaseResponse<UserDto>>

    @PATCH("user/me")
    suspend fun editUserInfo(
        @Body profileEditRequest : ProfileEditRequest
    ): Response<BaseResponse<ProfileEditResponse>>

    @POST("user/expert")
    suspend fun joinExpert(
        @Body joinExpertRequest: JoinExpertRequest
    ): Response<BaseResponse<MessageResponse>>

    @DELETE("user/me")
    suspend fun deleteUser(): Response<BaseResponse<MessageResponse>>

    @PATCH("user/me/pw")
    suspend fun pwdEdit(
        @Body pwdEditRequest: PwdEditRequest
    ): Response<BaseResponse<ProfileEditResponse>>

    @GET("email/check")
    suspend fun checkDuplEmail(
        @Query("email") userEmail: String
    ): Response<BaseResponse<CheckDuplResponse>>

    @GET("user/profile-image/download-url")
    suspend fun getImage(
        @Query("filename") fileName: String
    ): Response<BaseResponse<UrlResponse>>
}