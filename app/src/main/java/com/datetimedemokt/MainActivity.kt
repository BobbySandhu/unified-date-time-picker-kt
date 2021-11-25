package com.datetimedemokt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bobgenix.datetimedialogkt.AndroidUtilities
import com.bobgenix.datetimedialogkt.DialogHelper
import com.datetimedemokt.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AndroidUtilities.checkDisplaySize(this, resources.configuration)
        binding.buttonMy.setOnClickListener {
            Log.d("aaaa", "clicked.....")
            DialogHelper.createDatePickerDialog(
                this,
                -1,
                object : DialogHelper.ScheduleDatePickerDelegate {
                    override fun didSelectDate(notify: Boolean, scheduleDate: Long) {
                        /*val sdf = SimpleDateFormat(DATE_FORMAT_Z, Locale.ROOT)
                        val calendar: Calendar = Calendar.getInstance()
                        calendar.timeInMillis = scheduleDate.toLong()
                        Log.d("aaaa", "${sdf.format(calendar.time)}")*/
                    }
                }
            )
            Log.d("aaaa", "after clicked.....")
        }
    }
}