package com.example.sunseeker_app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface SunriseSunsetApi {
    @GET("json")
    suspend fun getSunTimes(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("formatted") formatted: Int = 0
    ): SunriseSunsetResponse
}
