/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bobgenix.datetimedialogkt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.text.TextUtils
import android.util.SparseArray
import android.util.TypedValue
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.bobgenix.datetimedialogkt.AndroidUtilities.BUTTON_DECREMENT
import com.bobgenix.datetimedialogkt.AndroidUtilities.BUTTON_INCREMENT
import com.bobgenix.datetimedialogkt.AndroidUtilities.dp
import java.util.*
import kotlin.math.abs

open class NumberPicker @JvmOverloads constructor(
    context: Context,
    textSize: Int = 18,
) : LinearLayout(context) {

    private var SELECTOR_WHEEL_ITEM_COUNT = 3
    private var SELECTOR_MIDDLE_ITEM_INDEX = SELECTOR_WHEEL_ITEM_COUNT / 2

    private val mTextSize: Int = dp(textSize.toFloat())

    private var textOffset = 0
    private var mInputText: TextView? = null
    private var mSelectionDividersDistance = 0
    private var mMinHeight = 0
    private var mMaxHeight = 0
    private var mMinWidth = 0
    private var mMaxWidth = 0
    private var mComputeMaxWidth = false
    private var mSelectorTextGapHeight = 0
    private var mMinValue = 0
    private var mMaxValue = 0
    private var mValue = 0
    private var mOnValueChangeListener: OnValueChangeListener? = null
    private var mOnScrollListener: OnScrollListener? = null
    private var mFormatter: Formatter? = null
    private var mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL
    private val mSelectorIndexToStringCache = SparseArray<String?>()
    private var mSelectorIndices = IntArray(SELECTOR_WHEEL_ITEM_COUNT)
    private var mSelectorWheelPaint: Paint? = null
    private var mSelectorElementHeight = 0
    private var mInitialScrollOffset = Int.MIN_VALUE
    private var mCurrentScrollOffset = 0
    private var mFlingScroller: Scroller? = null
    private var mAdjustScroller: Scroller? = null
    private var mPreviousScrollerY = 0
    private var mChangeCurrentByOneFromLongPressCommand: ChangeCurrentByOneFromLongPressCommand? =
        null
    private var mLastDownEventY = 0f
    private var mLastDownEventTime: Long = 0
    private var mLastDownOrMoveEventY = 0f
    private var mVelocityTracker: VelocityTracker? = null
    private var mTouchSlop = 0
    private var mMinimumFlingVelocity = 0
    private var mMaximumFlingVelocity = 0
    private var mWrapSelectorWheel = false
    private var mSolidColor = 0
    private var mSelectionDivider: Paint? = null
    private var mSelectionDividerHeight = 0
    private var mScrollState = OnScrollListener.SCROLL_STATE_IDLE
    private var mIngonreMoveEvents = false
    private var mTopSelectionDividerTop = 0
    private var mBottomSelectionDividerBottom = 0
    private val mLastHoveredChildVirtualViewId = 0
    private var mIncrementVirtualButtonPressed = false
    private var mDecrementVirtualButtonPressed = false
    private var mPressedStateHelper: PressedStateHelper? = null
    private var mLastHandledDownDpadKeyCode = -1
    private var accessibilityDelegate: SeekBarAccessibilityDelegate? = null
    private var drawDividers = true
    private val mDisplayedValues: Array<String>? = null

    interface OnValueChangeListener {
        fun onValueChange(picker: NumberPicker?, oldVal: Int, newVal: Int)
    }

    interface OnScrollListener {
        companion object {
            const val SCROLL_STATE_IDLE = 0
            const val SCROLL_STATE_TOUCH_SCROLL = 1
            const val SCROLL_STATE_FLING = 2
        }

        fun onScrollStateChange(view: NumberPicker?, scrollState: Int)
    }

    interface Formatter {
        fun format(value: Int): String
    }

    fun setItemCount(count: Int) {
        if (SELECTOR_WHEEL_ITEM_COUNT == count) {
            return
        }
        SELECTOR_WHEEL_ITEM_COUNT = count
        SELECTOR_MIDDLE_ITEM_INDEX = SELECTOR_WHEEL_ITEM_COUNT / 2
        mSelectorIndices = IntArray(SELECTOR_WHEEL_ITEM_COUNT)
        initializeSelectorWheelIndices()
    }

    private fun init() {
        mSolidColor = 0
        mSelectionDivider = Paint()
        mSelectionDivider?.color = Color.BLUE

        mSelectionDividerHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT.toFloat(),
            resources.displayMetrics
        ).toInt()
        mSelectionDividersDistance = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE.toFloat(),
            resources.displayMetrics
        ).toInt()

        mMinHeight = SIZE_UNSPECIFIED

        mMaxHeight =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 180f, resources.displayMetrics).toInt()
        require(!(mMinHeight != SIZE_UNSPECIFIED && mMaxHeight != SIZE_UNSPECIFIED && mMinHeight > mMaxHeight)) { "minHeight > maxHeight" }

        mMinWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, resources.displayMetrics).toInt()

        mMaxWidth = SIZE_UNSPECIFIED

        require(!(mMinWidth != SIZE_UNSPECIFIED && mMaxWidth != SIZE_UNSPECIFIED && mMinWidth > mMaxWidth)) { "minWidth > maxWidth" }

        mComputeMaxWidth = mMaxWidth == SIZE_UNSPECIFIED

        mPressedStateHelper = PressedStateHelper()

        setWillNotDraw(false)

        mInputText = TextView(context)

        mInputText?.let { inputText ->
            inputText.gravity = Gravity.CENTER
            inputText.isSingleLine = true
            inputText.setTextColor(Color.BLACK)
            inputText.setBackgroundResource(0)
            inputText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize.toFloat())
            inputText.visibility = INVISIBLE

            addView(
                inputText,
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }

        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumFlingVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumFlingVelocity = configuration.scaledMaximumFlingVelocity / SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT

        val paint = Paint()
        paint.isAntiAlias = true
        paint.textAlign = Align.CENTER
        paint.textSize = mTextSize.toFloat()
        paint.typeface = mInputText?.typeface
        val colors = mInputText!!.textColors
        val color = colors!!.getColorForState(ENABLED_STATE_SET, Color.WHITE)
        paint.color = color
        mSelectorWheelPaint = paint

        mFlingScroller = Scroller(context, null, true)
        mAdjustScroller = Scroller(context, DecelerateInterpolator(2.5f))

        updateInputTextView()

        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setAccessibilityDelegate(object : SeekBarAccessibilityDelegate() {
            override fun doScroll(host: View?, backward: Boolean) {
                changeValueByOne(!backward)
            }

            override fun canScrollBackward(host: View?): Boolean {
                return true
            }

            override fun canScrollForward(host: View?): Boolean {
                return true
            }

            override fun getContentDescription(host: View?): CharSequence {
                return this@NumberPicker.getContentDescription(mValue)
            }
        }.also { accessibilityDelegate = it })
    }

    protected open fun getContentDescription(value: Int): CharSequence {
        return mInputText?.text ?: ""
    }

    fun setTextColor(color: Int) {
        mInputText?.setTextColor(color)
        mSelectorWheelPaint?.color = color
    }

    fun setSelectorColor(color: Int) {
        mSelectionDivider?.color = context.resources.getColor(color, null)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val msrdWdth = measuredWidth
        val msrdHght = measuredHeight
        val inptTxtMsrdWdth = mInputText!!.measuredWidth
        val inptTxtMsrdHght = mInputText!!.measuredHeight
        val inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2
        val inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2
        val inptTxtRight = inptTxtLeft + inptTxtMsrdWdth
        val inptTxtBottom = inptTxtTop + inptTxtMsrdHght
        mInputText?.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom)

        if (changed) {
            initializeSelectorWheel()
            initializeFadingEdges()
            mTopSelectionDividerTop = (height - mSelectionDividersDistance) / 2 - mSelectionDividerHeight
            mBottomSelectionDividerBottom = mTopSelectionDividerTop + 2 * mSelectionDividerHeight + mSelectionDividersDistance
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, mMaxWidth)
        val newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight)
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec)
        val widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth, measuredWidth, widthMeasureSpec)
        val heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight, measuredHeight, heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
    }

    private fun moveToFinalScrollerPosition(scroller: Scroller?): Boolean {
        scroller?.forceFinished(true)
        var amountToScroll: Int = (scroller?.finalY ?: 0) - (scroller?.currY ?: 0)
        val futureScrollOffset = (mCurrentScrollOffset + amountToScroll) % mSelectorElementHeight
        var overshootAdjustment = mInitialScrollOffset - futureScrollOffset
        if (overshootAdjustment != 0) {
            if (Math.abs(overshootAdjustment) > mSelectorElementHeight / 2) {
                if (overshootAdjustment > 0) {
                    overshootAdjustment -= mSelectorElementHeight
                } else {
                    overshootAdjustment += mSelectorElementHeight
                }
            }
            amountToScroll += overshootAdjustment
            scrollBy(0, amountToScroll)
            return true
        }
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            removeAllCallbacks()
            mInputText?.visibility = INVISIBLE
            mLastDownEventY = event.y
            mLastDownOrMoveEventY = mLastDownEventY
            mLastDownEventTime = event.eventTime
            mIngonreMoveEvents = false

            if (mLastDownEventY < mTopSelectionDividerTop) {
                if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    mPressedStateHelper?.buttonPressDelayed(BUTTON_DECREMENT)
                }
            } else if (mLastDownEventY > mBottomSelectionDividerBottom) {
                if (mScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                    mPressedStateHelper?.buttonPressDelayed(BUTTON_INCREMENT)
                }
            }

            parent.requestDisallowInterceptTouchEvent(true)

            when {
                mFlingScroller?.isFinished != true -> {
                    mFlingScroller?.forceFinished(true)
                    mAdjustScroller?.forceFinished(true)
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
                }
                mAdjustScroller?.isFinished != true -> {
                    mFlingScroller?.forceFinished(true)
                    mAdjustScroller?.forceFinished(true)
                }
                mLastDownEventY < mTopSelectionDividerTop -> {
                    postChangeCurrentByOneFromLongPress(
                        false,
                        ViewConfiguration.getLongPressTimeout().toLong()
                    )
                }
                mLastDownEventY > mBottomSelectionDividerBottom -> {
                    postChangeCurrentByOneFromLongPress(
                        true,
                        ViewConfiguration.getLongPressTimeout().toLong()
                    )
                }
            }
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val currentMoveY = event.y
                if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    val deltaDownY = Math.abs(currentMoveY - mLastDownEventY).toInt()
                    if (deltaDownY > mTouchSlop) {
                        removeAllCallbacks()
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
                    }
                } else {
                    val deltaMoveY = (currentMoveY - mLastDownOrMoveEventY).toInt()
                    scrollBy(0, deltaMoveY)
                    invalidate()
                }
                mLastDownOrMoveEventY = currentMoveY
            }
            MotionEvent.ACTION_UP -> {
                removeChangeCurrentByOneFromLongPress()
                mPressedStateHelper!!.cancel()
                val velocityTracker = mVelocityTracker
                velocityTracker?.computeCurrentVelocity(1000, mMaximumFlingVelocity.toFloat())
                val initialVelocity = velocityTracker?.yVelocity?.toInt() ?: 0
                if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                    fling(initialVelocity)
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING)
                } else {
                    val eventY = event.y.toInt()
                    val deltaMoveY = abs(eventY - mLastDownEventY).toInt()
                    val deltaTime = event.eventTime - mLastDownEventTime

                    if (deltaMoveY <= mTouchSlop && deltaTime < ViewConfiguration.getTapTimeout()) {
                        val selectorIndexOffset =
                            eventY / mSelectorElementHeight - SELECTOR_MIDDLE_ITEM_INDEX
                        if (selectorIndexOffset > 0) {
                            changeValueByOne(true)
                            mPressedStateHelper?.buttonTapped(BUTTON_INCREMENT)
                        } else if (selectorIndexOffset < 0) {
                            changeValueByOne(false)
                            mPressedStateHelper?.buttonTapped(BUTTON_DECREMENT)
                        }
                    } else {
                        ensureScrollWheelAdjusted()
                    }
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
                }
                mVelocityTracker?.recycle()
                mVelocityTracker = null
            }
        }
        return true
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> removeAllCallbacks()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                removeAllCallbacks()
            }
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP -> {
                when (event.action) {
                    KeyEvent.ACTION_DOWN -> if (if (mWrapSelectorWheel || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) mValue < maxValue else mValue > minValue) {
                        requestFocus()
                        mLastHandledDownDpadKeyCode = keyCode
                        removeAllCallbacks()
                        if (mFlingScroller?.isFinished == true) {
                            changeValueByOne(keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                        }
                        return true
                    }
                    KeyEvent.ACTION_UP -> if (mLastHandledDownDpadKeyCode == keyCode) {
                        mLastHandledDownDpadKeyCode = -1
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> removeAllCallbacks()
        }
        return super.dispatchTrackballEvent(event)
    }

    override fun computeScroll() {
        var scroller: Scroller? = mFlingScroller
        if (scroller?.isFinished == true) {
            scroller = mAdjustScroller
            if (scroller?.isFinished == true) {
                return
            }
        }
        scroller?.computeScrollOffset()
        val currentScrollerY: Int = scroller?.currY ?: 0
        if (mPreviousScrollerY == 0) {
            mPreviousScrollerY = scroller?.startY ?: 0
        }
        scrollBy(0, currentScrollerY - mPreviousScrollerY)
        mPreviousScrollerY = currentScrollerY
        if (scroller?.isFinished == true) {
            onScrollerFinished(scroller)
        } else {
            invalidate()
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        mInputText?.isEnabled = enabled
    }

    override fun scrollBy(x: Int, y: Int) {
        val selectorIndices = mSelectorIndices

        if (!mWrapSelectorWheel && y > 0 && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] <= mMinValue && mCurrentScrollOffset + y > mInitialScrollOffset) {
            mCurrentScrollOffset = mInitialScrollOffset
            return
        }

        if (!mWrapSelectorWheel && y < 0 && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] >= mMaxValue && mCurrentScrollOffset + y < mInitialScrollOffset) {
            mCurrentScrollOffset = mInitialScrollOffset
            return
        }

        mCurrentScrollOffset += y

        while (mCurrentScrollOffset - mInitialScrollOffset > mSelectorTextGapHeight) {
            mCurrentScrollOffset -= mSelectorElementHeight
            decrementSelectorIndices(selectorIndices)
            setValueInternal(selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX], true)
            if (!mWrapSelectorWheel && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] <= mMinValue && mCurrentScrollOffset > mInitialScrollOffset) {
                mCurrentScrollOffset = mInitialScrollOffset
            }
        }

        while (mCurrentScrollOffset - mInitialScrollOffset < -mSelectorTextGapHeight) {
            mCurrentScrollOffset += mSelectorElementHeight
            incrementSelectorIndices(selectorIndices)
            setValueInternal(selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX], true)
            if (!mWrapSelectorWheel && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] >= mMaxValue && mCurrentScrollOffset < mInitialScrollOffset) {
                mCurrentScrollOffset = mInitialScrollOffset
            }
        }
    }

    override fun computeVerticalScrollOffset(): Int {
        return mCurrentScrollOffset
    }

    override fun computeVerticalScrollRange(): Int {
        return (mMaxValue - mMinValue + 1) * mSelectorElementHeight
    }

    override fun computeVerticalScrollExtent(): Int {
        return height
    }

    override fun getSolidColor(): Int {
        return mSolidColor
    }

    fun setOnValueChangedListener(onValueChangedListener: OnValueChangeListener?) {
        mOnValueChangeListener = onValueChangedListener
    }

    fun setOnScrollListener(onScrollListener: OnScrollListener?) {
        mOnScrollListener = onScrollListener
    }

    fun setFormatter(formatter: Formatter) {
        if (formatter == mFormatter) {
            return
        }
        mFormatter = formatter
        initializeSelectorWheelIndices()
        updateInputTextView()
    }

    fun setTextOffset(value: Int) {
        textOffset = value
        invalidate()
    }

    fun setValue(value: Int) {
        setValueInternal(value, false)
    }

    private fun tryComputeMaxWidth() {
        if (!mComputeMaxWidth) {
            return
        }
        var maxTextWidth = 0
        if (mDisplayedValues == null) {
            var maxDigitWidth = 0f
            for (i in 0..9) {
                val digitWidth = mSelectorWheelPaint!!.measureText(formatNumberWithLocale(i))
                if (digitWidth > maxDigitWidth) {
                    maxDigitWidth = digitWidth
                }
            }
            var numberOfDigits = 0
            var current = mMaxValue
            while (current > 0) {
                numberOfDigits++
                current = current / 10
            }
            maxTextWidth = (numberOfDigits * maxDigitWidth).toInt()
        } else {
            for (mDisplayedValue in mDisplayedValues) {
                val textWidth = mSelectorWheelPaint!!.measureText(mDisplayedValue)
                if (textWidth > maxTextWidth) {
                    maxTextWidth = textWidth.toInt()
                }
            }
        }
        maxTextWidth += mInputText!!.paddingLeft + mInputText!!.paddingRight
        if (mMaxWidth != maxTextWidth) {
            mMaxWidth = if (maxTextWidth > mMinWidth) {
                maxTextWidth
            } else {
                mMinWidth
            }
            invalidate()
        }
    }

    var wrapSelectorWheel: Boolean
        get() = mWrapSelectorWheel
        set(wrapSelectorWheel) {
            val wrappingAllowed = mMaxValue - mMinValue >= mSelectorIndices.size
            if ((!wrapSelectorWheel || wrappingAllowed) && wrapSelectorWheel != mWrapSelectorWheel) {
                mWrapSelectorWheel = wrapSelectorWheel
            }
        }

    fun setOnLongPressUpdateInterval(intervalMillis: Long) {
        mLongPressUpdateInterval = intervalMillis
    }

    fun getValue(): Int {
        return mValue
    }

    var minValue: Int
        get() = mMinValue
        set(minValue) {
            if (mMinValue == minValue) {
                return
            }
            require(minValue >= 0) { "minValue must be >= 0" }
            mMinValue = minValue
            if (mMinValue > mValue) {
                mValue = mMinValue
            }
            val wrapSelectorWheelValue = mMaxValue - mMinValue > mSelectorIndices.size
            mWrapSelectorWheel = wrapSelectorWheelValue
            initializeSelectorWheelIndices()
            updateInputTextView()
            tryComputeMaxWidth()
            invalidate()
        }
    var maxValue: Int
        get() = mMaxValue
        set(maxValue) {
            if (mMaxValue == maxValue) {
                return
            }
            require(maxValue >= 0) { "maxValue must be >= 0" }
            mMaxValue = maxValue
            if (mMaxValue < mValue) {
                mValue = mMaxValue
            }
            val wrapSelectorWheelValue = mMaxValue - mMinValue > mSelectorIndices.size
            wrapSelectorWheel = wrapSelectorWheelValue
            initializeSelectorWheelIndices()
            updateInputTextView()
            tryComputeMaxWidth()
            invalidate()
        }

    override fun getTopFadingEdgeStrength(): Float {
        return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeAllCallbacks()
    }

    override fun onDraw(canvas: Canvas) {
        val x = ((right - left) / 2 + textOffset).toFloat()
        var y = mCurrentScrollOffset.toFloat()

        // draw the selector wheel
        val selectorIndices = mSelectorIndices
        for (i in selectorIndices.indices) {
            val selectorIndex = selectorIndices[i]
            val scrollSelectorValue = mSelectorIndexToStringCache[selectorIndex]
            // Do not draw the middle item if input is visible since the input
            // is shown only if the wheel is static and it covers the middle
            // item. Otherwise, if the user starts editing the text via the
            // IME he may see a dimmed version of the old value intermixed
            // with the new one.
            if (scrollSelectorValue != null && (i != SELECTOR_MIDDLE_ITEM_INDEX || mInputText!!.visibility != VISIBLE)) {
                canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint!!)
            }
            y += mSelectorElementHeight.toFloat()
        }
        if (drawDividers) {
            val topOfTopDivider = mTopSelectionDividerTop
            val bottomOfTopDivider = topOfTopDivider + mSelectionDividerHeight
            canvas.drawRect(
                0f,
                topOfTopDivider.toFloat(),
                right.toFloat(),
                bottomOfTopDivider.toFloat(),
                mSelectionDivider!!
            )
            val bottomOfBottomDivider = mBottomSelectionDividerBottom
            val topOfBottomDivider = bottomOfBottomDivider - mSelectionDividerHeight
            canvas.drawRect(
                0f,
                topOfBottomDivider.toFloat(),
                right.toFloat(),
                bottomOfBottomDivider.toFloat(),
                mSelectionDivider!!
            )
        }
    }

    private fun makeMeasureSpec(measureSpec: Int, maxSize: Int): Int {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec
        }
        val size = MeasureSpec.getSize(measureSpec)
        return when (val mode = MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> measureSpec
            MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(
                Math.min(size, maxSize),
                MeasureSpec.EXACTLY
            )
            MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY)
            else -> throw IllegalArgumentException("Unknown measure mode: $mode")
        }
    }

    private fun resolveSizeAndStateRespectingMinSize(
        minSize: Int, measuredSize: Int, measureSpec: Int
    ): Int {
        return if (minSize != SIZE_UNSPECIFIED) {
            val desiredWidth = Math.max(minSize, measuredSize)
            resolveSizeAndState(
                desiredWidth,
                measureSpec,
                0
            )
        } else {
            measuredSize
        }
    }

    private fun initializeSelectorWheelIndices() {
        mSelectorIndexToStringCache.clear()
        val selectorIndices = mSelectorIndices
        val current = mValue
        for (i in mSelectorIndices.indices) {
            var selectorIndex = current + (i - SELECTOR_MIDDLE_ITEM_INDEX)
            if (mWrapSelectorWheel) {
                selectorIndex = getWrappedSelectorIndex(selectorIndex)
            }
            selectorIndices[i] = selectorIndex
            ensureCachedScrollSelectorValue(selectorIndices[i])
        }
    }

    private fun setValueInternal(currentValue: Int, notifyChange: Boolean) {
        var current = currentValue
        if (mValue == current) {
            return
        }
        if (mWrapSelectorWheel) {
            current = getWrappedSelectorIndex(current)
        } else {
            current = Math.max(current, mMinValue)
            current = Math.min(current, mMaxValue)
        }
        val previous = mValue
        mValue = current
        updateInputTextView()
        if (notifyChange) {
            notifyChange(previous, current)
        }
        initializeSelectorWheelIndices()
        invalidate()
    }

    private fun changeValueByOne(increment: Boolean) {
        mInputText!!.visibility = INVISIBLE
        if (!moveToFinalScrollerPosition(mFlingScroller)) {
            moveToFinalScrollerPosition(mAdjustScroller)
        }
        mPreviousScrollerY = 0
        if (increment) {
            mFlingScroller?.startScroll(0, 0, 0, -mSelectorElementHeight, SNAP_SCROLL_DURATION)
        } else {
            mFlingScroller?.startScroll(0, 0, 0, mSelectorElementHeight, SNAP_SCROLL_DURATION)
        }
        invalidate()
    }

    private fun initializeSelectorWheel() {
        initializeSelectorWheelIndices()
        val selectorIndices = mSelectorIndices
        val totalTextHeight = selectorIndices.size * mTextSize
        val totalTextGapHeight = (bottom - top - totalTextHeight).toFloat()
        val textGapCount = selectorIndices.size.toFloat()
        mSelectorTextGapHeight = (totalTextGapHeight / textGapCount + 0.5f).toInt()
        mSelectorElementHeight = mTextSize + mSelectorTextGapHeight
        val editTextTextPosition = mInputText!!.baseline + mInputText!!.top
        mInitialScrollOffset =
            editTextTextPosition - mSelectorElementHeight * SELECTOR_MIDDLE_ITEM_INDEX
        mCurrentScrollOffset = mInitialScrollOffset
        updateInputTextView()
    }

    private fun initializeFadingEdges() {
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength((bottom - top - mTextSize) / 2)
    }

    private fun onScrollerFinished(scroller: Scroller?) {
        if (scroller === mFlingScroller) {
            if (!ensureScrollWheelAdjusted()) {
                updateInputTextView()
            }
            onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE)
        } else {
            if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                updateInputTextView()
            }
        }
    }

    private fun onScrollStateChange(scrollState: Int) {
        if (mScrollState == scrollState) {
            return
        }
        mScrollState = scrollState
        if (mOnScrollListener != null) {
            mOnScrollListener?.onScrollStateChange(this, scrollState)
        }

        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            if (am.isTouchExplorationEnabled) {
                val text =
                    if (mDisplayedValues == null) formatNumber(mValue) else mDisplayedValues[mValue - mMinValue]
                val event = AccessibilityEvent.obtain()
                event.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                event.text.add(text)
                am.sendAccessibilityEvent(event)
            }
        }
    }

    private fun fling(velocityY: Int) {
        mPreviousScrollerY = 0
        if (velocityY > 0) {
            mFlingScroller?.fling(0, 0, 0, velocityY, 0, 0, 0, Int.MAX_VALUE)
        } else {
            mFlingScroller?.fling(0, Int.MAX_VALUE, 0, velocityY, 0, 0, 0, Int.MAX_VALUE)
        }
        invalidate()
    }

    private fun getWrappedSelectorIndex(selectorIndex: Int): Int {
        if (selectorIndex > mMaxValue) {
            return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1
        } else if (selectorIndex < mMinValue) {
            return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1
        }
        return selectorIndex
    }

    private fun incrementSelectorIndices(selectorIndices: IntArray) {
        System.arraycopy(selectorIndices, 1, selectorIndices, 0, selectorIndices.size - 1)
        var nextScrollSelectorIndex = selectorIndices[selectorIndices.size - 2] + 1
        if (mWrapSelectorWheel && nextScrollSelectorIndex > mMaxValue) {
            nextScrollSelectorIndex = mMinValue
        }
        selectorIndices[selectorIndices.size - 1] = nextScrollSelectorIndex
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex)
    }

    private fun decrementSelectorIndices(selectorIndices: IntArray) {
        System.arraycopy(selectorIndices, 0, selectorIndices, 1, selectorIndices.size - 1)
        var nextScrollSelectorIndex = selectorIndices[1] - 1
        if (mWrapSelectorWheel && nextScrollSelectorIndex < mMinValue) {
            nextScrollSelectorIndex = mMaxValue
        }
        selectorIndices[0] = nextScrollSelectorIndex
        ensureCachedScrollSelectorValue(nextScrollSelectorIndex)
    }

    private fun ensureCachedScrollSelectorValue(selectorIndex: Int) {
        val cache = mSelectorIndexToStringCache
        var scrollSelectorValue = cache[selectorIndex]
        if (scrollSelectorValue != null) {
            return
        }
        scrollSelectorValue = if (selectorIndex < mMinValue || selectorIndex > mMaxValue) {
            ""
        } else {
            if (mDisplayedValues != null) {
                val displayedValueIndex = selectorIndex - mMinValue
                mDisplayedValues[displayedValueIndex]
            } else {
                formatNumber(selectorIndex)
            }
        }
        cache.put(selectorIndex, scrollSelectorValue)
    }

    private fun formatNumber(value: Int): String {
        return if (mFormatter != null) mFormatter!!.format(value) else formatNumberWithLocale(value)
    }

    private fun updateInputTextView(): Boolean {
        val text =
            if (mDisplayedValues == null) formatNumber(mValue) else mDisplayedValues[mValue - mMinValue]
        if (!TextUtils.isEmpty(text) && text != mInputText!!.text.toString()) {
            mInputText!!.text = text
            return true
        }
        return false
    }

    private fun notifyChange(previous: Int, current: Int) {
        if (mOnValueChangeListener != null) {
            mOnValueChangeListener!!.onValueChange(this, previous, mValue)
        }
    }

    private fun postChangeCurrentByOneFromLongPress(increment: Boolean, delayMillis: Long) {
        if (mChangeCurrentByOneFromLongPressCommand == null) {
            mChangeCurrentByOneFromLongPressCommand = ChangeCurrentByOneFromLongPressCommand()
        } else {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand)
        }
        mChangeCurrentByOneFromLongPressCommand!!.setStep(increment)
        postDelayed(mChangeCurrentByOneFromLongPressCommand, delayMillis)
    }

    private fun removeChangeCurrentByOneFromLongPress() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand)
        }
    }

    private fun removeAllCallbacks() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand)
        }
        mPressedStateHelper?.cancel()
    }

    private fun getSelectedPos(value: String): Int {
        var value = value
        if (mDisplayedValues == null) {
            try {
                return value.toInt()
            } catch (e: NumberFormatException) {
                // Ignore as if it's not a number we don't care
            }
        } else {
            for (i in mDisplayedValues.indices) {
                // Don't force the user to type in jan when ja will do
                value = value.toLowerCase()
                if (mDisplayedValues[i].toLowerCase().startsWith(value)) {
                    return mMinValue + i
                }
            }

            /*
             * The user might have typed in a number into the month field i.e.
             * 10 instead of OCT so support that too.
             */try {
                return value.toInt()
            } catch (e: NumberFormatException) {
                // Ignore as if it's not a number we don't care
            }
        }
        return mMinValue
    }

    private fun ensureScrollWheelAdjusted(): Boolean {
        // adjust to the closest value
        var deltaY = mInitialScrollOffset - mCurrentScrollOffset
        if (deltaY != 0) {
            mPreviousScrollerY = 0
            if (Math.abs(deltaY) > mSelectorElementHeight / 2) {
                deltaY += if (deltaY > 0) -mSelectorElementHeight else mSelectorElementHeight
            }
            mAdjustScroller?.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS)
            invalidate()
            return true
        }
        return false
    }

    internal inner class PressedStateHelper : Runnable {
        private val MODE_PRESS = 1
        private val MODE_TAPPED = 2
        private var mManagedButton = 0
        private var mMode = 0


        fun cancel() {
            mMode = 0
            mManagedButton = 0
            removeCallbacks(this)
            if (mIncrementVirtualButtonPressed) {
                mIncrementVirtualButtonPressed = false
                invalidate()
            }
            mDecrementVirtualButtonPressed = false
            if (mDecrementVirtualButtonPressed) {
                invalidate()
            }
        }

        fun buttonPressDelayed(button: Int) {
            cancel()
            mMode = MODE_PRESS
            mManagedButton = button
            postDelayed(this, ViewConfiguration.getTapTimeout().toLong())
        }

        fun buttonTapped(button: Int) {
            cancel()
            mMode = MODE_TAPPED
            mManagedButton = button
            post(this)
        }

        override fun run() {
            when (mMode) {
                MODE_PRESS -> {
                    when (mManagedButton) {
                        BUTTON_INCREMENT -> {
                            mIncrementVirtualButtonPressed = true
                            invalidate(0, mBottomSelectionDividerBottom, right, bottom)
                        }
                        BUTTON_DECREMENT -> {
                            mDecrementVirtualButtonPressed = true
                            invalidate(0, 0, right, mTopSelectionDividerTop)
                        }
                    }
                }
                MODE_TAPPED -> {
                    when (mManagedButton) {
                        BUTTON_INCREMENT -> {
                            if (!mIncrementVirtualButtonPressed) {
                                postDelayed(
                                    this,
                                    ViewConfiguration.getPressedStateDuration().toLong()
                                )
                            }
                            mIncrementVirtualButtonPressed = mIncrementVirtualButtonPressed xor true
                            invalidate(0, mBottomSelectionDividerBottom, right, bottom)
                        }
                        BUTTON_DECREMENT -> {
                            if (!mDecrementVirtualButtonPressed) {
                                postDelayed(
                                    this,
                                    ViewConfiguration.getPressedStateDuration().toLong()
                                )
                            }
                            mDecrementVirtualButtonPressed = mDecrementVirtualButtonPressed xor true
                            invalidate(0, 0, right, mTopSelectionDividerTop)
                        }
                    }
                }
            }
        }
    }

    internal inner class ChangeCurrentByOneFromLongPressCommand : Runnable {
        private var mIncrement = false
        fun setStep(increment: Boolean) {
            mIncrement = increment
        }

        override fun run() {
            changeValueByOne(mIncrement)
            postDelayed(this, mLongPressUpdateInterval)
        }
    }

    fun setDrawDividers(drawDividers: Boolean) {
        this.drawDividers = drawDividers
        invalidate()
    }

    private fun getThemedColor(key: String): Int {
        return Color.GREEN
    }

    companion object {
        private const val DEFAULT_LONG_PRESS_UPDATE_INTERVAL: Long = 300
        private const val SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8
        private const val SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800
        private const val SNAP_SCROLL_DURATION = 300
        private const val TOP_AND_BOTTOM_FADING_EDGE_STRENGTH = 0.9f
        private const val UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT = 2
        private const val UNSCALED_DEFAULT_SELECTION_DIVIDERS_DISTANCE = 48
        private const val DEFAULT_LAYOUT_RESOURCE_ID = 0
        private const val SIZE_UNSPECIFIED = -1

        fun resolveSizeAndState(size: Int, measureSpec: Int, childMeasuredState: Int): Int {
            var result = size
            val specMode = MeasureSpec.getMode(measureSpec)
            val specSize = MeasureSpec.getSize(measureSpec)
            when (specMode) {
                MeasureSpec.UNSPECIFIED -> result = size
                MeasureSpec.AT_MOST -> result = if (specSize < size) {
                    specSize or 16777216
                } else {
                    size
                }
                MeasureSpec.EXACTLY -> result = specSize
            }
            return result or (childMeasuredState and -16777216)
        }

        private fun formatNumberWithLocale(value: Int): String {
            return String.format(Locale.getDefault(), "%d", value)
        }
    }

    init {
        init()
    }
}