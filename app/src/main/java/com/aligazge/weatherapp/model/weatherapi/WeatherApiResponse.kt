package com.aligazge.weatherapp.model.weatherapi

data class WeatherApiResponse(
    val location: Location,
    val current: Current
)