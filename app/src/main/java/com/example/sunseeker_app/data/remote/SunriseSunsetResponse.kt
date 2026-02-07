package com.example.sunseeker_app.data.remote

import com.squareup.moshi.Json


data class SunriseSunsetResponse(
    val results: SunriseSunsetResults,
    val status: String
)

data class SunriseSunsetResults(
    val sunrise: String,
    val sunset: String,
    @Json(name = "solar_noon") val solarNoon: String,
    @Json(name = "day_length") val dayLength: String,
    @Json(name = "civil_twilight_begin") val civilTwilightBegin: String,
    @Json(name = "civil_twilight_end") val civilTwilightEnd: String
)
