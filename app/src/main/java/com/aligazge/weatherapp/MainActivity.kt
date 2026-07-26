package com.aligazge.weatherapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aligazge.weatherapp.api.RetrofitClient
import com.aligazge.weatherapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.aligazge.weatherapp.adapter.HourlyWeatherAdapter
import com.aligazge.weatherapp.model.HourlyWeather
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.location.Geocoder
import java.util.Locale
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.aligazge.weatherapp.adapter.DailyWeatherAdapter
import com.aligazge.weatherapp.model.DailyWeather
import java.text.SimpleDateFormat


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getCurrentLocation()
            } else {
                Log.d("Location", "Location permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupHourlyForecast()
        setupSearch()

        binding.btnLocation.setOnClickListener {
            checkLocationPermission()
        }

        // Load weather for the default city when the app starts
        fetchWeather("Sharjah")
    }

    private fun setupSearch() {

        binding.etSearch.setOnEditorActionListener { _, _, _ ->

            val city = binding.etSearch.text.toString().trim()

            if (city.isNotEmpty()) {
                fetchWeather(city, clearSearch = true)
            }

            true
        }
    }

    private fun checkLocationPermission() {

        when {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {

                getCurrentLocation()
            }

            else -> {

                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    private fun fetchWeather(city: String, clearSearch: Boolean = false) {

        lifecycleScope.launch {

            try {

                val response = RetrofitClient.api.getCurrentWeather(
                    city = city,
                    apiKey = BuildConfig.WEATHER_API_KEY
                )

                if (response.isSuccessful) {

                    val weather = response.body()

                    if (weather != null) {

                        binding.tvCity.text = weather.name
                        binding.tvTemperature.text = "${weather.main.temp.toInt()}°"
                        binding.tvCondition.text = weather.weather[0].main
                        updateWeatherTheme(
                            weather.weather[0].main,
                            weather.weather[0].icon
                        )
                        binding.tvHighLow.text =
                            "H:${weather.main.temp_max.toInt()}°   L:${weather.main.temp_min.toInt()}°"

                        fetchForecast(weather.name)

                        if (clearSearch) {
                            binding.etSearch.text?.clear()
                        }
                    }

                } else {

                    Log.e(
                        "WeatherAPI",
                        "Error: ${response.code()} ${response.message()}"
                    )

                }

            } catch (e: Exception) {

                Log.e("WeatherAPI", "Exception: ${e.message}", e)

            }
        }
    }

    private fun fetchWeatherByLocation(
        latitude: Double,
        longitude: Double
    ) {

        lifecycleScope.launch {

            try {

                val response = RetrofitClient.api.getCurrentWeatherByLocation(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = BuildConfig.WEATHER_API_KEY
                )

                if (response.isSuccessful) {

                    val weather = response.body()

                    if (weather != null) {

                        binding.tvCity.text = weather.name
                        binding.tvTemperature.text = "${weather.main.temp.toInt()}°"
                        binding.tvCondition.text = weather.weather[0].main
                        updateWeatherTheme(
                            weather.weather[0].main,
                            weather.weather[0].icon
                        )
                        binding.tvHighLow.text =
                            "H:${weather.main.temp_max.toInt()}°   L:${weather.main.temp_min.toInt()}°"

                        fetchForecast(weather.name)
                    }

                } else {

                    Log.e(
                        "WeatherAPI",
                        "Error: ${response.code()} ${response.message()}"
                    )

                }

            } catch (e: Exception) {

                Log.e("WeatherAPI", "Exception: ${e.message}", e)

            }
        }
    }

    private fun getWeatherIcon(condition: String): Int {
        return when (condition) {
            "Clear" -> R.drawable.ic_sunny
            "Clouds" -> R.drawable.ic_cloud
            "Rain" -> R.drawable.ic_rain
            else -> R.drawable.ic_cloud
        }
    }

    private fun updateWeatherTheme(condition: String,icon: String ) {

        when (condition) {

            "Clear" -> {

                if (icon.endsWith("n")) {
                    binding.main.setBackgroundResource(R.drawable.background_night)
                    binding.weatherAnimation.setAnimation(R.raw.night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_sunny)
                    binding.weatherAnimation.setAnimation(R.raw.sunny)
                }
            }

            "Clouds" -> {
                binding.main.setBackgroundResource(R.drawable.background_cloudy)
                binding.weatherAnimation.setAnimation(R.raw.cloud)
            }

            "Rain", "Drizzle" -> {
                binding.main.setBackgroundResource(R.drawable.background_rainy)
                binding.weatherAnimation.setAnimation(R.raw.rain)
            }

            "Thunderstorm" -> {
                binding.main.setBackgroundResource(R.drawable.background_storm)
                binding.weatherAnimation.setAnimation(R.raw.storm)
            }

            "Snow" -> {
                binding.main.setBackgroundResource(R.drawable.background_snow)
                binding.weatherAnimation.setAnimation(R.raw.snow)
            }

            "Mist", "Fog", "Haze", "Smoke" -> {
                binding.main.setBackgroundResource(R.drawable.background_cloudy)
                binding.weatherAnimation.setAnimation(R.raw.cloud)
            }

            else -> {
                binding.main.setBackgroundResource(R.drawable.background_sunny)
                binding.weatherAnimation.setAnimation(R.raw.sunny)
            }
        }

        binding.weatherAnimation.playAnimation()
    }


    private fun fetchForecast(city: String) {

        lifecycleScope.launch {

            try {

                val response = RetrofitClient.api.getForecast(
                    city = city,
                    apiKey = BuildConfig.WEATHER_API_KEY
                )

                if (response.isSuccessful) {

                    val forecast = response.body()

                    forecast?.let {

                        val inputFormat = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        )

                        val outputFormat = SimpleDateFormat(
                            "h a",
                            Locale.getDefault()
                        )

                        // Hourly Forecast
                        val hourlyList = it.list.take(5).map { item ->

                            val date = inputFormat.parse(item.dt_txt)
                            val time = outputFormat.format(date!!)

                            HourlyWeather(
                                time = time,
                                temperature = "${item.main.temp.toInt()}°",
                                icon = getWeatherIcon(item.weather[0].main)
                            )
                        }

                        binding.rvHourlyWeather.adapter =
                            HourlyWeatherAdapter(hourlyList)

                        // Daily Forecast
                        val dailyList = mutableListOf<DailyWeather>()

                        val groupedForecast = it.list.groupBy {
                            it.dt_txt.substring(0, 10)
                        }

                        groupedForecast.entries.take(5).forEach { entry ->

                            val forecasts = entry.value

                            val high = forecasts.maxOf { forecast ->
                                forecast.main.temp
                            }

                            val low = forecasts.minOf { forecast ->
                                forecast.main.temp
                            }

                            val firstForecast = forecasts.first()

                            val day = SimpleDateFormat(
                                "EEE",
                                Locale.getDefault()
                            ).format(
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).parse(entry.key)!!
                            )

                            dailyList.add(
                                DailyWeather(
                                    day = day,
                                    condition = firstForecast.weather[0].main,
                                    highTemp = "${high.toInt()}°",
                                    lowTemp = "${low.toInt()}°",
                                    icon = getWeatherIcon(firstForecast.weather[0].main)
                                )
                            )
                        }

                        binding.rvDailyWeather.layoutManager =
                            LinearLayoutManager(this@MainActivity)

                        binding.rvDailyWeather.adapter =
                            DailyWeatherAdapter(dailyList)
                    }

                } else {

                    Log.e(
                        "Forecast",
                        "Error: ${response.code()} ${response.message()}"
                    )

                }

            } catch (e: Exception) {

                Log.e("Forecast", "Exception: ${e.message}", e)

            }
        }
    }

    private fun setupHourlyForecast() {

        val hourlyList = listOf(
            HourlyWeather("9 AM", "28°", R.drawable.ic_sunny),
            HourlyWeather("12 PM", "31°", R.drawable.ic_cloud),
            HourlyWeather("3 PM", "30°", R.drawable.ic_sunny),
            HourlyWeather("6 PM", "27°", R.drawable.ic_rain),
            HourlyWeather("9 PM", "25°", R.drawable.ic_cloud)
        )

        binding.rvHourlyWeather.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.rvHourlyWeather.adapter =
            HourlyWeatherAdapter(hourlyList)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        )
            .addOnSuccessListener { location ->

                if (location != null) {

                    val geocoder = Geocoder(this, Locale.getDefault())

                    val addresses = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )

                    if (!addresses.isNullOrEmpty()) {

                        val city = addresses[0].locality

                        Log.d("Location", "Latitude: ${location.latitude}")
                        Log.d("Location", "Longitude: ${location.longitude}")
                        Log.d("Location", "City: $city")

                        fetchWeatherByLocation(
                            location.latitude,
                            location.longitude
                        )
                    }


                } else {
                    Log.d("Location", "Location is null")
                }

            }
            .addOnFailureListener { e ->
                Log.e("Location", "Failed to get location", e)
            }
    }
}