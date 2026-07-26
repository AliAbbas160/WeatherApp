package com.aligazge.weatherapp.model

data class ForecastItem(
    val dt_txt: String,
    val main: ForecastMain,
    val weather: List<ForecastWeather>
)