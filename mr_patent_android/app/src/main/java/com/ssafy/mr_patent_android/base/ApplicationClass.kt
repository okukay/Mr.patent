package com.ssafy.mr_patent_android.base

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.ACCESS_TOKEN
import com.ssafy.mr_patent_android.base.ApplicationClass.Companion.sharedPreferences
import com.ssafy.mr_patent_android.data.remote.NetworkUtil
import com.ssafy.mr_patent_android.util.SharedPreferencesUtil
import okhttp3.Cookie
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Url
import java.io.IOException
import java.net.CookieManager
import java.util.concurrent.TimeUnit

// 앱이 실행될때 1번만 실행이 됩니다.
class ApplicationClass : Application() {
    val API_URL = "https://j12d208.p.ssafy.io/api/"
    val API_URL_FAST = "https://j12d208.p.ssafy.io/fastapi/"

    companion object {
        lateinit var sharedPreferences: SharedPreferencesUtil

        const val ACCESS_TOKEN = "ACCESS-TOKEN"
        const val REFRESH_TOKEN = "REFRESH-TOKEN"
        const val SHARED_PREFERENCES_NAME = "MR_PATENT"
//        const val COOKIES_KEY_NAME = "cookies"

        lateinit var retrofit: Retrofit
        lateinit var retrofit_fast: Retrofit

        lateinit var instance: ApplicationClass
            private set

        val networkUtil = NetworkUtil
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = SharedPreferencesUtil(applicationContext)

        initRetrofitInstance()
    }

    private fun initRetrofitInstance() {
        val client: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addNetworkInterceptor(XAccessTokenInterceptor())
            .authenticator(ReissueInterceptor(instance.applicationContext))
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val client_fast: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addNetworkInterceptor(XAccessTokenInterceptor())
            .authenticator(ReissueInterceptor(instance.applicationContext))
            .build()

        retrofit_fast = Retrofit.Builder()
            .baseUrl(API_URL_FAST)
            .client(client_fast)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val gson : Gson = GsonBuilder()
        .setLenient()
        .create()
}

class XAccessTokenInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val jwtToken: String? = sharedPreferences.getAToken()
        if (chain.request().url.toString()
                .contains("mr-patent.s3.ap-northeast-2.amazonaws.com")
        ) {
            return chain.proceed(builder.build())
        }else {
            if (!jwtToken.isNullOrEmpty()) {
                builder.addHeader("Authorization", "Bearer $jwtToken")
            } else {
                Log.w("TOKEN_WARNING", "JWT 토큰이 존재하지 않습니다!")
            }
            return chain.proceed(builder.build())
        }
    }
}