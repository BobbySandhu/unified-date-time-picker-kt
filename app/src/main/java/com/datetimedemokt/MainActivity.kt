package com.datetimedemokt

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bobgenix.datetimedialogkt.OnDateTimeSelectedListener
import com.bobgenix.datetimedialogkt.UnifiedDateTimePicker
import com.datetimedemokt.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val DATE_FORMAT_Z = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.buttonMy.setOnClickListener {
            Log.d("aaaa", "clicked.....")

            UnifiedDateTimePicker.Builder(context = this)
                .title("Select Date and time")
                .vibration(true)
                .buttonColor(Color.BLACK)
                .addListener(object : OnDateTimeSelectedListener {
                    override fun onDateTimeSelected(millis: Long) {
                        val sdf = SimpleDateFormat(DATE_FORMAT_Z, Locale.ROOT)
                        val calendar: Calendar = Calendar.getInstance()
                        calendar.timeInMillis = millis
                        Log.d("aaaa", "${sdf.format(calendar.time)}")
                    }
                })
                .show()

            Log.d("aaaa", "after clicked.....")
        }
    }
}