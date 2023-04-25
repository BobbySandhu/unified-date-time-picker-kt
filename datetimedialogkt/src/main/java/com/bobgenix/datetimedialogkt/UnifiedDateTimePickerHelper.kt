@file:Suppress("SameParameterValue")

package com.bobgenix.datetimedialogkt

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.util.StateSet
import android.util.TypedValue
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.bobgenix.datetimedialogkt.AndroidUtilities.dp
import com.bobgenix.datetimedialogkt.Constants.DATE_FORMAT_LONG
import com.bobgenix.datetimedialogkt.Constants.DATE_FORMAT_SHORT
import java.util.*

internal class UnifiedDateTimePickerHelper(private val unifiedDateTimePicker: UnifiedDateTimePicker) {

    var locale: Locale = unifiedDateTimePicker.locale ?: Locale.getDefault() ?: Locale.ENGLISH
    var formatterScheduleDay = createFormatter(locale, DATE_FORMAT_SHORT, DATE_FORMAT_SHORT)
    var formatterScheduleYear = createFormatter(locale, DATE_FORMAT_LONG, DATE_FORMAT_LONG)
    var formatterScheduleSend = arrayOfNulls<FastDateFormat>(15)

    fun createDatePickerDialog(
        context: Context = unifiedDateTimePicker.context,
        currentDateValue: Long = -1,
    ): BottomSheet.Builder {

        AndroidUtilities.checkDisplaySize(
            unifiedDateTimePicker.context,
            unifiedDateTimePicker.context.resources.configuration
        )

        formatterScheduleSend[0] =
            createFormatter(
                locale,
                unifiedDateTimePicker.todayDateFormat,
                unifiedDateTimePicker.todayDateFormat
            )
        formatterScheduleSend[1] =
            createFormatter(
                locale,
                unifiedDateTimePicker.laterMonthDateFormat,
                unifiedDateTimePicker.laterMonthDateFormat
            )
        formatterScheduleSend[2] = createFormatter(
            locale,
            unifiedDateTimePicker.laterYearDateFormat,
            unifiedDateTimePicker.laterYearDateFormat
        )

        val builder = BottomSheet.Builder(context, false)
        builder.setApplyBottomPadding(false)

        val dayPicker = NumberPicker(context)
        dayPicker.setTextColor(
            ContextCompat.getColor(
                context,
                unifiedDateTimePicker.dateTimeTextColor
            )
        )
        dayPicker.setTextOffset(dp(10f))
        dayPicker.setItemCount(5)
        dayPicker.setSelectorColor(unifiedDateTimePicker.buttonColor)

        val hourPicker: NumberPicker = object : NumberPicker(context) {
            override fun getContentDescription(value: Int): CharSequence {
                return "Hours"
            }
        }
        hourPicker.setItemCount(5)
        hourPicker.setTextColor(
            ContextCompat.getColor(
                context,
                unifiedDateTimePicker.dateTimeTextColor
            )
        )
        hourPicker.setTextOffset(-dp(10f))
        hourPicker.setSelectorColor(unifiedDateTimePicker.buttonColor)

        val minutePicker: NumberPicker = object : NumberPicker(context) {
            override fun getContentDescription(value: Int): CharSequence {
                return "Minutes"
            }
        }
        minutePicker.setItemCount(5)
        minutePicker.setTextColor(
            ContextCompat.getColor(
                context,
                unifiedDateTimePicker.dateTimeTextColor
            )
        )
        minutePicker.setTextOffset(-dp(34f))
        minutePicker.setSelectorColor(unifiedDateTimePicker.buttonColor)

        val container: LinearLayout = object : LinearLayout(context) {
            var ignoreLayout = false
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                ignoreLayout = true

                val count: Int =
                    if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
                        3
                    } else {
                        5
                    }

                dayPicker.setItemCount(count)
                hourPicker.setItemCount(count)
                minutePicker.setItemCount(count)
                dayPicker.layoutParams.height = dp(54f) * count
                hourPicker.layoutParams.height = dp(54f) * count
                minutePicker.layoutParams.height = dp(54f) * count
                ignoreLayout = false
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }

            override fun requestLayout() {
                if (ignoreLayout) {
                    return
                }
                super.requestLayout()
            }
        }

        container.orientation = LinearLayout.VERTICAL

        val titleLayout = FrameLayout(context)
        container.addView(
            titleLayout,
            createLinear(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.TOP,
                22,
                0,
                0,
                4
            )
        )

        val titleView = TextView(context)
        titleView.text = unifiedDateTimePicker.title
        titleView.setTextColor(
            ContextCompat.getColor(
                context,
                unifiedDateTimePicker.titleTextColor

            )
        )
        titleView.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            unifiedDateTimePicker.titleTextSize.toFloat()
        )
        titleView.typeface = unifiedDateTimePicker.titleTypeface
        titleLayout.addView(
            titleView,
            createFrame(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT.toFloat(),
                Gravity.START or Gravity.TOP,
                0f,
                12f,
                0f,
                0f
            )
        )
        titleView.setOnTouchListener { v: View?, event: MotionEvent? -> true }

        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.weightSum = 1.0f
        container.addView(
            linearLayout,
            createLinear(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        val currentYear = calendar[Calendar.YEAR]
        val buttonTextView: TextView = object : AppCompatTextView(context) {
            override fun getAccessibilityClassName(): CharSequence {
                return Button::class.java.name
            }
        }

        linearLayout.addView(dayPicker, createLinear(0, 54 * 5, 0.5f))

        dayPicker.minValue = 0
        dayPicker.maxValue = 365
        dayPicker.wrapSelectorWheel = false
        dayPicker.setFormatter(object : NumberPicker.Formatter {
            override fun format(value: Int): String {
                return if (value == 0) {
                    unifiedDateTimePicker.todayText
                } else {
                    val date = currentTime + value.toLong() * 86400000L
                    calendar.timeInMillis = date
                    val year = calendar[Calendar.YEAR]
                    return if (year == currentYear) {
                        formatterScheduleDay.format(date)
                    } else {
                        formatterScheduleYear.format(date)
                    }
                }
            }
        })

        val onValueChangeListener = object : NumberPicker.OnValueChangeListener {
            override fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int) {
                try {
                    if (unifiedDateTimePicker.enableVibration) {
                        container.performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                    }
                } catch (ignore: Exception) {
                }
                checkScheduleDate(buttonTextView, null, 0, dayPicker, hourPicker, minutePicker)
            }
        }

        dayPicker.setOnValueChangedListener(onValueChangeListener)

        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        linearLayout.addView(hourPicker, createLinear(0, 54 * 5, 0.2f))
        hourPicker.setFormatter(object : NumberPicker.Formatter {
            override fun format(value: Int): String {
                return String.format("%02d", value)
            }
        })
        hourPicker.setOnValueChangedListener(onValueChangeListener)

        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.setValue(0)
        minutePicker.setFormatter(object : NumberPicker.Formatter {
            override fun format(value: Int): String {
                return String.format("%02d", value)
            }
        })
        linearLayout.addView(minutePicker, createLinear(0, 54 * 5, 0.3f))
        minutePicker.setOnValueChangedListener(onValueChangeListener)

        if (unifiedDateTimePicker.milliseconds > 0L && unifiedDateTimePicker.milliseconds > currentTime) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            calendar[Calendar.HOUR_OF_DAY] = 0
            val days =
                (((unifiedDateTimePicker.milliseconds - currentTime) / (24 * 60 * 60 * 1000))).toInt()
            calendar.timeInMillis = unifiedDateTimePicker.milliseconds
            if (days >= 0) {
                minutePicker.setValue(calendar[Calendar.MINUTE])
                hourPicker.setValue(calendar[Calendar.HOUR_OF_DAY])
                dayPicker.setValue(days)
            }
        }

        val canceled = booleanArrayOf(true)

        checkScheduleDate(buttonTextView, null, 0, dayPicker, hourPicker, minutePicker)

        buttonTextView.setPadding(dp(34f), 0, dp(34f), 0)
        buttonTextView.gravity = Gravity.CENTER
        buttonTextView.setTextColor(
            ContextCompat.getColor(
                context,
                unifiedDateTimePicker.buttonTextColor
            )
        )
        buttonTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            unifiedDateTimePicker.buttonTextSize.toFloat()
        )
        buttonTextView.typeface = unifiedDateTimePicker.buttonTypeface
        buttonTextView.setBackgroundDrawable(
            createSimpleSelectorRoundRectDrawable(
                dp(4f),
                unifiedDateTimePicker.buttonColor,
                Color.GRAY
            )
        )
        container.addView(
            buttonTextView,
            createLinear(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48,
                Gravity.START or Gravity.BOTTOM,
                16,
                15,
                16,
                16
            )
        )

        buttonTextView.setOnClickListener { _: View? ->
            canceled[0] = false
            val setSeconds =
                checkScheduleDate(null, null, 0, dayPicker, hourPicker, minutePicker)
            calendar.timeInMillis =
                System.currentTimeMillis() + dayPicker.getValue().toLong() * 24 * 3600 * 1000
            calendar[Calendar.HOUR_OF_DAY] = hourPicker.getValue()
            calendar[Calendar.MINUTE] = minutePicker.getValue()
            if (setSeconds) {
                calendar[Calendar.SECOND] = 0
            }
            unifiedDateTimePicker.onDateTimeSelected?.onDateTimeSelected(calendar.timeInMillis)
            builder.dismissRunnable.run()
        }

        builder.setCustomView(container)
        val bottomSheet = builder.show()
        //bottomSheet.setOnDismissListener { dialog: DialogInterface? -> Log.d("aaaa", "dismissed") }
        bottomSheet.setBackgroundColor(
            unifiedDateTimePicker.backgroundColor
        )

        return builder
    }

    private fun createLinear(
        width: Int,
        height: Int,
        gravity: Int,
        leftMargin: Int,
        topMargin: Int,
        rightMargin: Int,
        bottomMargin: Int
    ): LinearLayout.LayoutParams {
        val layoutParams =
            LinearLayout.LayoutParams(getSize(width.toFloat()), getSize(height.toFloat()))
        layoutParams.setMargins(
            dp(leftMargin.toFloat()),
            dp(topMargin.toFloat()),
            dp(rightMargin.toFloat()),
            dp(bottomMargin.toFloat())
        )
        layoutParams.gravity = gravity

        return layoutParams
    }

    private fun createFrame(
        width: Int,
        height: Float,
        gravity: Int,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): FrameLayout.LayoutParams {
        val layoutParams =
            FrameLayout.LayoutParams(getSize(width.toFloat()), getSize(height), gravity)
        layoutParams.setMargins(dp(leftMargin), dp(topMargin), dp(rightMargin), dp(bottomMargin))
        return layoutParams
    }

    private fun getSize(size: Float): Int {
        return (if (size < 0) size else dp(size)).toInt()
    }

    private fun createFormatter(
        locale: Locale,
        formatValue: String,
        defaultFormat: String
    ): FastDateFormat {
        var format: String? = formatValue
        if (format.isNullOrEmpty()) {
            format = defaultFormat
        }
        var formatter: FastDateFormat
        try {
            formatter = FastDateFormat.getInstance(format, locale)
        } catch (e: Exception) {
            format = defaultFormat
            formatter = FastDateFormat.getInstance(format, locale)
        }
        return formatter
    }

    fun checkScheduleDate(
        button: TextView?,
        infoText: TextView?,
        type: Int,
        dayPicker: NumberPicker,
        hourPicker: NumberPicker,
        minutePicker: NumberPicker
    ): Boolean {
        return checkScheduleDate(button, infoText, 0, type, dayPicker, hourPicker, minutePicker)
    }

    private fun checkScheduleDate(
        button: TextView?,
        infoText: TextView?,
        maxDateValue: Long,
        type: Int,
        dayPicker: NumberPicker,
        hourPicker: NumberPicker,
        minutePicker: NumberPicker
    ): Boolean {

        var maxDate = maxDateValue

        var day = dayPicker.getValue()
        var hour = hourPicker.getValue()
        var minute = minutePicker.getValue()
        val calendar = Calendar.getInstance()

        val systemTime = System.currentTimeMillis()
        calendar.timeInMillis = systemTime
        val currentYear = calendar[Calendar.YEAR]
        val currentDay = calendar[Calendar.DAY_OF_YEAR]

        if (maxDate > 0) {
            maxDate *= 1000
            calendar.timeInMillis = systemTime + maxDate
            calendar[Calendar.HOUR_OF_DAY] = 23
            calendar[Calendar.MINUTE] = 59
            calendar[Calendar.SECOND] = 59
            maxDate = calendar.timeInMillis
        }

        calendar.timeInMillis = System.currentTimeMillis() + day.toLong() * 24 * 3600 * 1000
        calendar[Calendar.HOUR_OF_DAY] = hour
        calendar[Calendar.MINUTE] = minute
        val currentTime = calendar.timeInMillis

        if (currentTime <= systemTime + 60000L) {
            calendar.timeInMillis = systemTime + 60000L

            if (currentDay != calendar[Calendar.DAY_OF_YEAR]) {
                day = 1
                dayPicker.setValue(day)
            }

            hour = calendar[Calendar.HOUR_OF_DAY]
            minute = calendar[Calendar.MINUTE]

            hourPicker.setValue(hour)
            minutePicker.setValue(minute)
        } else if (maxDate in 1 until currentTime) {
            calendar.timeInMillis = maxDate
            day = 7
            hour = calendar[Calendar.HOUR_OF_DAY]
            minute = calendar[Calendar.MINUTE]

            dayPicker.setValue(day)
            hourPicker.setValue(hour)
            minutePicker.setValue(minute)
        }

        val selectedYear = calendar[Calendar.YEAR]

        calendar.timeInMillis = System.currentTimeMillis() + day.toLong() * 24 * 3600 * 1000
        calendar[Calendar.HOUR_OF_DAY] = hour
        calendar[Calendar.MINUTE] = minute

        val time = calendar.timeInMillis

        if (button != null) {
            var num: Int = when {
                day == 0 -> {
                    0
                }

                currentYear == selectedYear -> {
                    1
                }

                else -> {
                    2
                }
            }
            when (type) {
                1 -> {
                    num += 3
                }

                2 -> {
                    num += 6
                }

                3 -> {
                    num += 9
                }
            }

            button.text = formatterScheduleSend[num]?.format(time)
        }

        return currentTime - systemTime > 60000L
    }

    private fun createLinear(width: Int, height: Int, weight: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            getSize(width.toFloat()),
            getSize(height.toFloat()),
            weight
        )
    }

    private fun createLinear(width: Int, height: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(getSize(width.toFloat()), getSize(height.toFloat()))
    }

    @JvmOverloads
    fun createSimpleSelectorRoundRectDrawable(
        rad: Int,
        defaultColor: Int,
        pressedColor: Int,
        maskColor: Int = pressedColor
    ): Drawable {
        val defaultDrawable = ShapeDrawable(
            RoundRectShape(
                floatArrayOf(
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat()
                ), null, null
            )
        )
        defaultDrawable.paint.color =
            ContextCompat.getColor(
                unifiedDateTimePicker.context, defaultColor
            )
        val pressedDrawable = ShapeDrawable(
            RoundRectShape(
                floatArrayOf(
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat(),
                    rad.toFloat()
                ), null, null
            )
        )
        pressedDrawable.paint.color = maskColor
        val colorStateList = ColorStateList(
            arrayOf(StateSet.WILD_CARD),
            intArrayOf(pressedColor)
        )
        return RippleDrawable(colorStateList, defaultDrawable, pressedDrawable)
    }


}