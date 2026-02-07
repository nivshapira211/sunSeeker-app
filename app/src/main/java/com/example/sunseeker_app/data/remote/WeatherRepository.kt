package com.example.sunseeker_app.data.remote

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val api: SunriseSunsetApi
) {
    suspend fun fetchSolarData(latitude: Double, longitude: Double): SolarResult {
        return try {
            val response = api.getSunTimes(latitude, longitude)
            if (response.status.equals("OK", ignoreCase = true)) {
                val results = response.results
                SolarResult.Success(
                    SolarData(
                        sunrise = results.sunrise,
                        sunset = results.sunset,
                        solarNoon = results.solarNoon,
                        dayLength = results.dayLength,
                        civilTwilightBegin = results.civilTwilightBegin,
                        civilTwilightEnd = results.civilTwilightEnd
                    )
                )
            } else {
                SolarResult.Error("API status: ${response.status}")
            }
        } catch (e: Exception) {
            SolarResult.Error(e.message ?: "Network error")
        }
    }
}

data class SolarData(
    val sunrise: String,
    val sunset: String,
    val solarNoon: String,
    val dayLength: String,
    val civilTwilightBegin: String,
    val civilTwilightEnd: String
)

sealed class SolarResult {
    data class Success(val data: SolarData) : SolarResult()
    data class Error(val message: String) : SolarResult()
}
