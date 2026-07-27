package com.aligazge.weatherapp.model.weatherapi

import com.google.gson.annotations.SerializedName

data class Hour(

    val time: String,

    @SerializedName("temp_c")
    val tempC: Double,

    val condition: Condition

)