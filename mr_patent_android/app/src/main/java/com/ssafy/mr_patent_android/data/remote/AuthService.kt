package com.ssafy.mr_patent_android.data.remote

import com.ssafy.mr_patent_android.base.BaseResponse
import com.ssafy.mr_patent_android.data.model.dto.FcmRequest
import com.ssafy.mr_patent_android.data.model.dto.LoginRequest
import com.ssafy.mr_patent_android.data.model.dto.LogoutRequest
import com.ssafy.mr_patent_android.data.model.dto.MessageDto
import com.ssafy.mr_patent_android.data.model.response.LoginResponse
import com.ssafy.mr_patent_android.data.model.dto.PwdChangeRequest
import com.ssafy.mr_patent_android.data.model.dto.UserDto
import com.ssafy.mr_patent_android.data.model.response.MessageResponse
import com.ssafy.mr_patent_android.data.model.response.VerifyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthService {
    @POST("user/reissue")
    suspend fun reissue(
        @Body refreshToken: String
    ): Response<BaseResponse<LoginResponse>>

    @POST("user/logout")
    suspend fun logout(
        @Body logoutRequest: LogoutRequest
    ): Response<BaseResponse<MessageResponse>>

    @POST("user/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<BaseResponse<LoginResponse>>

    @POST("fcm/token")
    suspend fun sendFcmToken(
        @Body fcmRequest: FcmRequest
    ): Response<BaseResponse<Boolean>>

    @POST("email/verification")
    suspend fun emailVerify(
        @Query("email") userEmail: String,
        @Query("authCode") authCode: String
    ): Response<BaseResponse<VerifyResponse>>

    @POST("email/request")
    suspend fun sendCode(
        @Query("email") userEmail: String
    ): Response<BaseResponse<VerifyResponse>>

    @POST("email/password/forgot")
    suspend fun sendForgotCode(
        @Body user_email: PwdChangeRequest
    ): Response<BaseResponse<VerifyResponse>>

    @POST("email/password/verification")
    suspend fun sendCodePwd(
        @Body user_email: PwdChangeRequest
    ): Response<BaseResponse<MessageDto>>

    @POST("email/password/reset")
    suspend fun changePassword(
        @Body pwdChangeRequest: PwdChangeRequest
    ): Response<BaseResponse<MessageDto>>


    @DELETE("fcm/token/delete/{userId}")
    suspend fun deleteFcmToken(
        @Path("userId") userId: Int
    ): Response<BaseResponse<Boolean>>
}