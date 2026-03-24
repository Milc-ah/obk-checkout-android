package com.example.obkcheckout

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

object QairosRetrofit {
    private const val BASE_URL = "https://apps.ensembleconsultinggroup.com/QairosDataServerOBK/QairosOBK/api/"
    private const val TAG = "QairosRetrofit"

    // HTTP logging interceptor — logs full request/response in Logcat
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp client with timeouts and logging
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)   // time to establish connection
        .readTimeout(30, TimeUnit.SECONDS)       // time to read server response
        .writeTimeout(30, TimeUnit.SECONDS)      // time to send request body
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("QairosRetrofit", "REQUEST URL: ${request.url}")
            Log.d("QairosRetrofit", "REQUEST METHOD: ${request.method}")
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val api: ApiQairosService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            // ScalarsConverterFactory MUST come first — handles plain String responses
            // (used by api/login which returns a raw string token/challenge)
            .addConverterFactory(ScalarsConverterFactory.create())
            // GsonConverterFactory handles JSON object responses
            // (used by api/container/{id} which returns ContainerLookupResponse)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiQairosService::class.java)
    }
}