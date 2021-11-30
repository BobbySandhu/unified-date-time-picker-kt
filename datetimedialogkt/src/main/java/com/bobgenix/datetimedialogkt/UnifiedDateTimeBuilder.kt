package com.bobgenix.datetimedialogkt

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

class UnifiedDateTimeBuilder {
    internal var context: Context

    internal var mTitle: String? = null
    internal var mBackgroundColor: Int = Color.WHITE
    internal var mButtonColor: Int = Color.BLUE
    internal var mTitleTypeface: Typeface
    internal var mButtonTypeface: Typeface
    internal var enableVibration: Boolean = true
    internal var mTitleTextSize: Int = 20
    internal var mButtonTextSize: Int = 14

    constructor(ctx: Context) {
        context = ctx
        mTitleTypeface = ResourcesCompat.getFont(context, R.font.roboto_medium)!!
        mButtonTypeface = mTitleTypeface
    }

    fun title(title: String): UnifiedDateTimeBuilder {
        mTitle = title
        return this
    }

    fun titleFont(typeface: Typeface): UnifiedDateTimeBuilder {
        mTitleTypeface = typeface
        return this
    }

    fun buttonFont(typeface: Typeface): UnifiedDateTimeBuilder {
        mButtonTypeface = typeface
        return this
    }

    fun backgroundColor(color: Int): UnifiedDateTimeBuilder {
        mBackgroundColor = color
        return this
    }

    fun buttonColor(color: Int): UnifiedDateTimeBuilder {
        mButtonColor = color
        return this
    }

    fun vibration(enable: Boolean): UnifiedDateTimeBuilder {
        enableVibration = enable
        return this
    }

    fun textSizeTitle(size: Int): UnifiedDateTimeBuilder {
        mTitleTextSize = AndroidUtilities.dp(size.toFloat())
        return this
    }

    fun textSizeButton(size: Int): UnifiedDateTimeBuilder {
        mButtonTextSize = AndroidUtilities.dp(size.toFloat())
        return this
    }
}