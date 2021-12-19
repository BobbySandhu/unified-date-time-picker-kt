package com.bobgenix.datetimedialogkt

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

class UnifiedDateTimePicker private constructor(
    internal var context: Context,
    internal var title: String,
    internal var backgroundColor: Int,
    internal var buttonColor: Int,
    internal var dateTimeTextColor: Int,
    internal var titleTextColor: Int,
    internal var buttonTextColor: Int,
    internal var titleTypeface: Typeface,
    internal var buttonTypeface: Typeface,
    internal var enableVibration: Boolean = true,
    internal var titleTextSize: Int = 20,
    internal var buttonTextSize: Int = 14,
    internal var onDateTimeSelected: OnDateTimeSelectedListener? = null,
    internal var milliseconds: Long = 0L
) {

    data class Builder(
        internal var context: Context,
        internal var title: String = "",
        internal var backgroundColor: Int = Color.WHITE,
        internal var buttonColor: Int = Color.BLUE,
        internal var dateTimeTextColor: Int = Color.BLACK,
        internal var titleTextColor: Int = Color.BLACK,
        internal var buttonTextColor: Int = Color.WHITE,
        internal var titleTypeface: Typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)!!,
        internal var buttonTypeface: Typeface = ResourcesCompat.getFont(context, R.font.roboto_medium)!!,
        internal var enableVibration: Boolean = true,
        internal var titleTextSize: Int = 20,
        internal var buttonTextSize: Int = 14,
        internal var onDateTimeSelected: OnDateTimeSelectedListener? = null,
        internal var milliseconds: Long = 0L
    ) {
        fun title(title: String) = apply {
            this.title = title
        }

        fun titleFont(typeface: Typeface) = apply {
            titleTypeface = typeface
        }

        fun buttonFont(typeface: Typeface) = apply {
            buttonTypeface = typeface
        }

        fun backgroundColor(color: Int) = apply {
            backgroundColor = color
        }

        fun buttonColor(color: Int) = apply {
            buttonColor = color
        }

        fun dateTimeTextColor(color: Int) = apply {
            dateTimeTextColor = color
        }

        fun titleTextColor(color: Int) = apply {
            titleTextColor = color
        }

        fun buttonTextColor(color: Int) = apply {
            buttonTextColor = color
        }

        fun vibration(enable: Boolean) = apply {
            enableVibration = enable
        }

        fun textSizeTitle(size: Int) = apply {
            titleTextSize = AndroidUtilities.dp(size.toFloat())
        }

        fun textSizeButton(size: Int) = apply {
            buttonTextSize = AndroidUtilities.dp(size.toFloat())
        }

        fun addListener(listener: OnDateTimeSelectedListener) = apply {
            onDateTimeSelected = listener
        }

        fun setDateTimeMillis(millis: Long) = apply {
            milliseconds = millis
        }

        private fun build() = UnifiedDateTimePicker(
            context,
            title,
            backgroundColor,
            buttonColor,
            dateTimeTextColor,
            titleTextColor,
            buttonTextColor,
            titleTypeface,
            buttonTypeface,
            enableVibration,
            titleTextSize,
            buttonTextSize,
            onDateTimeSelected,
            milliseconds
        )

        fun show() {
            val unifiedDateTimePicker = build()
            UnifiedDateTimePickerHelper(unifiedDateTimePicker).createDatePickerDialog()
        }
    }
}