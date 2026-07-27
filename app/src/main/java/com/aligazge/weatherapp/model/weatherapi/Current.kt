package com.aligazge.weatherapp.model.weatherapi

import com.google.gson.annotations.SerializedName

data class Current(

    @SerializedName("temp_c")
    val tempC: Double,

    @SerializedName("feelslike_c")
    val feelsLikeC: Double,

    @SerializedName("wind_kph")
    val windKph: Double,

    @SerializedName("pressure_mb")
    val pressureMb: Double,

    @SerializedName("humidity")
    val humidity: Int,

    @SerializedName("vis_km")
    val visibilityKm: Double,

    @SerializedName("cloud")
    val cloud: Int,

    @SerializedName("uv")
    val uv: Double,

    val condition: Condition,

    @SerializedName("air_quality")
    val airQuality: AirQuality,

    @SerializedName("is_day")
    val isDay: Int,

)