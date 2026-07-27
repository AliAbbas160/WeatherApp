package com.aligazge.weatherapp.model.weatherapi

import com.google.gson.annotations.SerializedName

data class AirQuality(

    @SerializedName("us-epa-index")
    val usEpaIndex: Int

)