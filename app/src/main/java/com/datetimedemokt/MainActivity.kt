package com.datetimedemokt

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bobgenix.datetimedialogkt.OnDateTimeSelectedListener
import com.bobgenix.datetimedialogkt.UnifiedDateTimePicker
import com.datetimedemokt.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val DATE_FORMAT = "yyyy-MMM-dd HH:mm"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.buttonMy.setOnClickListener {

            UnifiedDateTimePicker.Builder(context = this)
                .title("Select Date and time")
                .titleTextColor(R.color.white)
                .backgroundColor(R.color.big_stone)
                .dateTimeTextColor(R.color.white)
                .buttonColor(R.color.picton_blue)
                .buttonTextColor(R.color.white)
                .addListener(object : OnDateTimeSelectedListener {
                    override fun onDateTimeSelected(millis: Long) {
                        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.ROOT)
                        val calendar: Calendar = Calendar.getInstance()
                        calendar.timeInMillis = millis
                        binding.textSelectedDate.text = "${sdf.format(calendar.time)}"
                    }
                })
                .show()
        }
    }
}