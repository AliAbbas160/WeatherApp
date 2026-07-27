package com.aligazge.weatherapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import android.widget.Toast
import android.view.View
import android.content.SharedPreferences
import java.util.Date
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.aligazge.weatherapp.api.WeatherApiClient
import com.aligazge.weatherapp.util.Constants


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var sharedPreferences: SharedPreferences

    private var lastCity = "Sharjah"
    private var isUsingLocation = false

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

        sharedPreferences = getSharedPreferences("WeatherPrefs", MODE_PRIVATE)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupHourlyForecast()
        setupSearch()

        binding.swipeRefresh.setOnRefreshListener {

            if (isUsingLocation) {
                getCurrentLocation()
            } else {
                fetchWeather(lastCity)
            }
        }

        binding.btnLocation.setOnClickListener {
            checkLocationPermission()
        }

        // Load last searched city
        val useLocation = sharedPreferences.getBoolean("use_location", false)
        val lastCity = sharedPreferences.getString("last_city", "Sharjah")

        if (useLocation) {
            checkLocationPermission()
        } else {
            fetchWeather(lastCity ?: "Sharjah")
        }

    }

    private fun setupSearch() {

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {

                val city = binding.etSearch.text.toString().trim()

                if (city.isNotEmpty()) {
                    hideKeyboard()
                    fetchWeather(city, clearSearch = true)
                }

                true
            } else {
                false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    private fun formatTime(timestamp: Long, timezoneOffset: Int): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(Date((timestamp + timezoneOffset) * 1000L))
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

    private fun fetchWeather(location: String,clearSearch: Boolean = false,isLocation: Boolean = false
    ){

        if (!isLocation) {
            lastCity = location
        }

        isUsingLocation = isLocation

        binding.loadingOverlay.visibility = View.VISIBLE
        lifecycleScope.launch {

            try {

                val response = WeatherApiClient.api.getCurrentWeather(
                    apiKey = Constants.WEATHER_API_KEY,
                    location = location
                )

                if (response.isSuccessful) {

                    val weather = response.body()

                    if (weather != null) {

                        binding.tvCity.text = weather.location.name

                        sharedPreferences.edit()
                            .putString("last_city", weather.location.name)
                            .putBoolean("use_location", isLocation)
                            .apply()

                        binding.tvTemperature.text = "${weather.current.tempC.toInt()}°"

                        binding.tvCondition.text = weather.current.condition.text

                        binding.tvFeelsLike.text =
                            "${weather.current.feelsLikeC.toInt()}°C"

                        binding.tvHumidity.text =
                            "${weather.current.humidity}%"

                        binding.tvWindSpeed.text =
                            "${weather.current.windKph.toInt()} km/h"

                        binding.tvPressure.text =
                            "${weather.current.pressureMb.toInt()} hPa"

                        binding.tvVisibility.text =
                            "${weather.current.visibilityKm.toInt()} km"

                        binding.tvCloudiness.text =
                            "${weather.current.cloud}%"

                        binding.tvAirQuality.text =
                            weather.current.airQuality.usEpaIndex.toString()

                        updateWeatherTheme(
                            weather.current.condition.text,
                            weather.current.isDay == 1
                        )

                        fetchForecast(weather.location.name)

                        if (clearSearch) {
                            binding.etSearch.text?.clear()
                        }

                    }

                } else {

                    Log.e("WeatherAPI", response.errorBody()?.string() ?: "Unknown Error")

                }

            } catch (e: Exception) {

                Log.e("WeatherAPI", e.message ?: "Exception", e)

                Toast.makeText(
                    this@MainActivity,
                    "Failed to load weather",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {

                binding.loadingOverlay.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

            }

        }
    }

    private fun fetchWeatherByLocation(
        latitude: Double,
        longitude: Double
    ) {

        fetchWeather(
            location = "$latitude,$longitude",
            isLocation = true
        )

    }

    private fun getWeatherIcon(condition: String): Int {

        val weather = condition.lowercase(Locale.getDefault())

        return when {

            weather.contains("sun") || weather.contains("clear") ->
                R.drawable.ic_sunny

            weather.contains("cloud") || weather.contains("overcast") ->
                R.drawable.ic_cloud

            weather.contains("rain") || weather.contains("drizzle") ->
                R.drawable.ic_rain

            weather.contains("snow")
                    || weather.contains("blizzard")
                    || weather.contains("ice")
                    || weather.contains("sleet") ->
                R.drawable.ic_cloud

            weather.contains("thunder") ->
                R.drawable.ic_rain

            weather.contains("mist")
                    || weather.contains("fog")
                    || weather.contains("haze") ->
                R.drawable.ic_cloud

            else ->
                R.drawable.ic_cloud
        }
    }

    private fun updateWeatherTheme(condition: String,isDay: Boolean) {

        val weather = condition.lowercase(Locale.getDefault())

        when {

            weather.contains("sun") || weather.contains("clear") -> {

                if (!isDay) {
                    binding.main.setBackgroundResource(R.drawable.background_night)
                    binding.weatherAnimation.setAnimation(R.raw.night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_sunny)
                    binding.weatherAnimation.setAnimation(R.raw.sunny)
                }
            }

            weather.contains("cloud") || weather.contains("overcast") -> {

                if (!isDay) {
                    binding.main.setBackgroundResource(R.drawable.background_night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_cloudy)
                }

                binding.weatherAnimation.setAnimation(R.raw.cloud)
            }

            weather.contains("rain") || weather.contains("drizzle") -> {

                if (!isDay) {
                    binding.main.setBackgroundResource(R.drawable.background_night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_rainy)
                }

                binding.weatherAnimation.setAnimation(R.raw.rain)
            }

            weather.contains("thunder") -> {

                if (!isDay){
                    binding.main.setBackgroundResource(R.drawable.background_night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_storm)
                }

                binding.weatherAnimation.setAnimation(R.raw.storm)
            }

            weather.contains("snow")
                    || weather.contains("blizzard")
                    || weather.contains("sleet")
                    || weather.contains("ice") -> {

                if (!isDay){
                    binding.main.setBackgroundResource(R.drawable.background_night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_snow)
                }

                binding.weatherAnimation.setAnimation(R.raw.snow)
            }

            weather.contains("mist")
                    || weather.contains("fog")
                    || weather.contains("haze")
                    || weather.contains("smoke")
                    || weather.contains("dust")
                    || weather.contains("sand") -> {

                if (!isDay) {
                    binding.main.setBackgroundResource(R.drawable.background_night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_cloudy)
                }

                binding.weatherAnimation.setAnimation(R.raw.cloud)
            }

            else -> {

                if (!isDay){
                    binding.main.setBackgroundResource(R.drawable.background_night)
                    binding.weatherAnimation.setAnimation(R.raw.night)
                } else {
                    binding.main.setBackgroundResource(R.drawable.background_sunny)
                    binding.weatherAnimation.setAnimation(R.raw.sunny)
                }
            }
        }

        binding.weatherAnimation.playAnimation()
    }


    private fun fetchForecast(city: String) {

        lifecycleScope.launch {

            try {

                val response = WeatherApiClient.api.getForecast(
                    apiKey = Constants.WEATHER_API_KEY,
                    location = city
                )

                if (response.isSuccessful) {

                    val forecast = response.body()

                    forecast?.let {

                        val today = it.forecast.forecastday.first()

                        val hourlyList = today.hour
                            .take(5)
                            .map { hour ->

                                val time = hour.time.substringAfter(" ")

                                HourlyWeather(
                                    time = time,
                                    temperature = "${hour.tempC.toInt()}°",
                                    icon = getWeatherIcon(hour.condition.text)
                                )
                            }

                        val dailyList = it.forecast.forecastday.map { forecastDay ->

                            val day = SimpleDateFormat(
                                "EEE",
                                Locale.getDefault()
                            ).format(
                                SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).parse(forecastDay.date)!!
                            )

                            DailyWeather(
                                day = day,
                                condition = forecastDay.day.condition.text,
                                highTemp = "${forecastDay.day.maxTemp.toInt()}°",
                                lowTemp = "${forecastDay.day.minTemp.toInt()}°",
                                icon = getWeatherIcon(forecastDay.day.condition.text)
                            )
                        }

                        binding.rvDailyWeather.layoutManager =
                            LinearLayoutManager(this@MainActivity)

                        binding.rvDailyWeather.adapter =
                            DailyWeatherAdapter(dailyList)

                        binding.rvHourlyWeather.adapter =
                            HourlyWeatherAdapter(hourlyList)

                        binding.tvHighLow.text =
                            "H:${today.day.maxTemp.toInt()}°   L:${today.day.minTemp.toInt()}°"

                        binding.tvSunrise.text = today.astro.sunrise
                        binding.tvSunset.text = today.astro.sunset

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