package com.aligazge.weatherapp.model.weatherapi

data class ForecastDay(
    val date: String,
    val day: Day,
    val astro: Astro,
    val hour: List<Hour>
)