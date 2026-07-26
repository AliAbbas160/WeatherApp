package com.aligazge.weatherapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aligazge.weatherapp.databinding.ItemDailyWeatherBinding
import com.aligazge.weatherapp.model.DailyWeather

class DailyWeatherAdapter(
    private val dailyList: List<DailyWeather>
) : RecyclerView.Adapter<DailyWeatherAdapter.DailyViewHolder>() {

    inner class DailyViewHolder(
        private val binding: ItemDailyWeatherBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dailyWeather: DailyWeather) {

            binding.tvDay.text = dailyWeather.day
            binding.tvCondition.text = dailyWeather.condition
            binding.tvHighLow.text =
                "${dailyWeather.highTemp} / ${dailyWeather.lowTemp}"

            binding.imgDailyWeather.setImageResource(
                dailyWeather.icon
            )
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DailyViewHolder {

        val binding = ItemDailyWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DailyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DailyViewHolder,
        position: Int
    ) {
        holder.bind(dailyList[position])
    }

    override fun getItemCount() = dailyList.size
}