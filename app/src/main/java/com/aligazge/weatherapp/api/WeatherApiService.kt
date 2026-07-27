package com.aligazge.weatherapp.api

import com.aligazge.weatherapp.model.weatherapi.WeatherApiResponse
import com.aligazge.weatherapp.model.weatherapi.ForecastResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("current.json")
    suspend fun getCurrentWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("aqi") aqi: String = "yes"
    ): Response<WeatherApiResponse>

    @GET("forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("days") days: Int = 5,
        @Query("aqi") aqi: String = "yes",
        @Query("alerts") alerts: String = "no"
    ): Response<ForecastResponse>

}