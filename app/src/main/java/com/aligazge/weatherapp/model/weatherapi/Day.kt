package com.aligazge.weatherapp.model.weatherapi

import com.google.gson.annotations.SerializedName

data class Day(

    @SerializedName("maxtemp_c")
    val maxTemp: Double,

    @SerializedName("mintemp_c")
    val minTemp: Double,

    val condition: Condition

)