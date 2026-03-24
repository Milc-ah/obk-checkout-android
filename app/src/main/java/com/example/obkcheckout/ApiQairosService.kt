package com.example.obkcheckout

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiQairosService {

    // -------------------------------------------------------------------------
    // Auth — challenge/response login (no token needed)
    // -------------------------------------------------------------------------

    @GET("login/")
    suspend fun login(
        @Query("userId") userEmail: String,
        @Query("userChallenge") userChallenge: String?
    ): Response<String>

    // -------------------------------------------------------------------------
    // Get Record
    // -------------------------------------------------------------------------
    @GET("restBase/{className}/{id}")
    suspend fun getRecord(
        @Header("Authorization") authToken: String,
        @Path("className") className: String,
        @Path("id") id : Int,
        @QueryMap parameters: Map<String, String>?
    ): Response<String> //Json String

    // -------------------------------------------------------------------------
    // Get Records
    // -------------------------------------------------------------------------
    @GET("restBase/{className}")
    suspend fun getRecords(
        @Header("Authorization") token: String,
        @Path("className") className: String,
        @QueryMap parameters: Map<String, String>?
    ): Response<com.example.obkcheckout.Utility.Response>

    // -------------------------------------------------------------------------
    // Checkout — submit completed session
    // -------------------------------------------------------------------------

    @POST("api/checkout/confirm")
    suspend fun confirmCheckout(
        @Header("Authorization") token: String,
        @Body request: ConfirmCheckoutRequest
    ): Response<ConfirmCheckoutResponse>
}