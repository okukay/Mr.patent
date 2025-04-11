package com.ssafy.mr_patent_android.base

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.remote.RetrofitUtil.Companion.authService
import com.ssafy.mr_patent_android.ui.login.LoginActivity
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import kotlin.math.log

private const val TAG = "ReissueInterceptor"


class ReissueInterceptor(val context: Context) : Authenticator {
    val intent= Intent(context, LoginActivity::class.java)

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.i(TAG, "토큰 재발급 시도")
        if (response.request.url.encodedPath.contains("/reissue")) {
            Log.e(TAG, "토큰 재발급 요청도 401 발생 → 로그아웃 처리")
            goLoginActivity()
            return null
        }
        return runBlocking {
            try {
                val token = sharedPreferences.getRToken()

                val res = authService.reissue(token ?: "")
                if (res.isSuccessful) {
                    val newToken = res.body()?.data.let {
                        it?.accessToken
                    }

                    val newRToken = res.body()?.data.let {
                        it?.refreshToken
                    }
                    ApplicationClass.sharedPreferences.addRToken(newRToken?:"")
                    if (!newToken.isNullOrBlank()) {
                        return@runBlocking response.request.newBuilder()
                            .header("ACCESS-TOKEN", "Bearer $newToken")
                            .build()
                    }
                } else {
                    goLoginActivity()
                }
            } catch (e: Exception) {
                Log.e(TAG, "토큰 재발급 중 오류 발생", e)
                goLoginActivity()
            }
            goLoginActivity()

            null
        }
    }

    fun goLoginActivity() {
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}
