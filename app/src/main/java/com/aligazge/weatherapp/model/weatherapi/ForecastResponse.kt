package com.aligazge.weatherapp.model.weatherapi

data class ForecastResponse(
    val location: Location,
    val current: Current,
    val forecast: Forecast
)