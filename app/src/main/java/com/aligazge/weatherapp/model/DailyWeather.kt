package com.aligazge.weatherapp.model

data class DailyWeather(
    val day: String,
    val condition: String,
    val highTemp: String,
    val lowTemp: String,
    val icon: Int
)