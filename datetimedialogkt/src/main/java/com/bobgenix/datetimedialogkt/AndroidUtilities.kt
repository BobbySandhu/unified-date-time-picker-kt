package com.bobgenix.datetimedialogkt

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.util.DisplayMetrics
import android.util.StateSet
import android.view.*
import android.widget.FrameLayout
import java.lang.reflect.Field

object AndroidUtilities {
    var statusBarHeight = 0
    var density = 1f
    var displayMetrics: DisplayMetrics = DisplayMetrics()
    var displaySize = Point()

    const val BUTTON_INCREMENT = 1
    const val BUTTON_DECREMENT = 2

    fun dp(value: Float): Int {
        return if (value == 0f) {
            0
        } else Math.ceil((density * value).toDouble())
            .toInt()
    }

    fun fillStatusBarHeight(context: Context?) {
        if (context == null || statusBarHeight > 0) {
            return
        }
        statusBarHeight = getStatusBarHeight(context)
    }

    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    fun getPixelsInCM(cm: Float, isX: Boolean): Float {
        return cm / 2.54f * if (isX) displayMetrics.xdpi else displayMetrics.ydpi
    }

    private var mAttachInfoField: Field? = null
    private var mStableInsetsField: Field? = null

    fun getViewInset(view: View?): Int {
        if (view == null || Build.VERSION.SDK_INT < 21 || view.height == displaySize.y || view.height == displaySize.y - statusBarHeight) {
            return 0
        }
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                val insets: WindowInsets? = view.rootWindowInsets
                return if (insets != null) insets.getStableInsetBottom() else 0
            } else {
                if (mAttachInfoField == null) {
                    mAttachInfoField = View::class.java.getDeclaredField("mAttachInfo")
                    mAttachInfoField?.setAccessible(true)
                }
                val mAttachInfo = mAttachInfoField!![view]
                if (mAttachInfo != null) {
                    if (mStableInsetsField == null) {
                        mStableInsetsField = mAttachInfo.javaClass.getDeclaredField("mStableInsets")
                        mStableInsetsField?.setAccessible(true)
                    }
                    val insets = mStableInsetsField!![mAttachInfo] as Rect
                    return insets.bottom
                }
            }
        } catch (e: Exception) {
        }
        return 0
    }

    fun createFrame(width: Int, height: Int, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            getSize(width.toFloat()),
            getSize(height.toFloat()),
            gravity
        )
    }

    fun createFrame(width: Int, height: Float): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(getSize(width.toFloat()), getSize(height))
    }

    fun createFrame(
        width: Int,
        height: Float,
        gravity: Int,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): FrameLayout.LayoutParams {
        val layoutParams: FrameLayout.LayoutParams =
            FrameLayout.LayoutParams(getSize(width.toFloat()), getSize(height), gravity)
        layoutParams.setMargins(dp(leftMargin), dp(topMargin), dp(rightMargin), dp(bottomMargin))
        return layoutParams
    }

    private fun getSize(size: Float): Int {
        return (if (size < 0) size else dp(size)).toInt()
    }

    fun setLightNavigationBar(window: Window, enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            flags = if (enable) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
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
        defaultDrawable.getPaint().setColor(defaultColor)
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
        pressedDrawable.getPaint().setColor(maskColor)
        return if (Build.VERSION.SDK_INT >= 21) {
            val colorStateList = ColorStateList(
                arrayOf(StateSet.WILD_CARD),
                intArrayOf(pressedColor)
            )
            RippleDrawable(colorStateList, defaultDrawable, pressedDrawable)
        } else {
            val stateListDrawable = StateListDrawable()
            stateListDrawable.addState(intArrayOf(R.attr.state_pressed), pressedDrawable)
            stateListDrawable.addState(intArrayOf(R.attr.state_selected), pressedDrawable)
            stateListDrawable.addState(StateSet.WILD_CARD, defaultDrawable)
            stateListDrawable
        }
    }

    fun checkDisplaySize(context: Context, newConfiguration: Configuration?) {
        try {
            val oldDensity = density
            density = context.resources.displayMetrics.density
            val newDensity = density
            /*if (firstConfigurationWas && Math.abs(oldDensity - newDensity) > 0.001) {
                Theme.reloadAllResources(context);
            }
            firstConfigurationWas = true;*/
            var configuration = newConfiguration
            if (configuration == null) {
                configuration = context.resources.configuration
            }
            //usingHardwareInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            val manager: WindowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            if (manager != null) {
                val display: Display = manager.getDefaultDisplay()
                if (display != null) {
                    display.getMetrics(displayMetrics)
                    display.getSize(displaySize)
                    //screenRefreshRate = display.getRefreshRate();
                }
            }
            if (configuration!!.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
                val newSize = Math.ceil((configuration.screenWidthDp * density).toDouble())
                    .toInt()
                if (Math.abs(displaySize.x - newSize) > 3) {
                    displaySize.x = newSize
                }
            }
            if (configuration.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
                val newSize = Math.ceil((configuration.screenHeightDp * density).toDouble())
                    .toInt()
                if (Math.abs(displaySize.y - newSize) > 3) {
                    displaySize.y = newSize
                }
            }
            /*if (roundMessageSize == 0) {
                if (AndroidUtilities.isTablet()) {
                    roundMessageSize = (int) (AndroidUtilities.getMinTabletSide() * 0.6f);
                    roundPlayingMessageSize = (int) (AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(28));
                } else {
                    roundMessageSize = (int) (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) * 0.6f);
                    roundPlayingMessageSize = (int) (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y)  - AndroidUtilities.dp(28));
                }
                roundMessageInset = dp(2);
            }
            if (BuildVars.LOGS_ENABLED) {
                FileLog.e("density = " + density + " display size = " + displaySize.x + " " + displaySize.y + " " + displayMetrics.xdpi + "x" + displayMetrics.ydpi);
            }*/
        } catch (e: Exception) {
            //FileLog.e(e);
        }
    }
}