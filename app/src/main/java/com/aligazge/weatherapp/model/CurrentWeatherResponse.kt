package com.aligazge.weatherapp.model

data class CurrentWeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val visibility: Int,
    val clouds: Clouds,
    val sys: Sys,
    val timezone: Int
)

data class Main(
    val temp: Double,
    val temp_min: Double,
    val temp_max: Double,
    val humidity: Int,
    val feels_like: Double,
    val pressure: Int
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Double
)

data class Clouds(
    val all: Int
)

data class Sys(
    val country: String,
    val sunrise: Long,
    val sunset: Long
)