package com.template.retrofit


import okhttp3.ResponseBody
import retrofit2.http.GET


interface UrlAPI {
    @GET(".")
    suspend fun getURL(): ResponseBody
}