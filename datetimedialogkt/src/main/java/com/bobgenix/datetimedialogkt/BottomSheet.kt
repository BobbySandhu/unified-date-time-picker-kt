package com.bobgenix.datetimedialogkt

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.bobgenix.datetimedialogkt.AndroidUtilities.createFrame
import kotlin.math.abs

internal class BottomSheet(context: Context) : Dialog(context, R.style.TransparentDialog) {

    private var shadowDrawable: Drawable? = null
    private var backgroundPaddingLeft: Int
    private var backgroundPaddingTop: Int
    private var touchSlop: Int
    private var container: ContainerView? = null
    private val allowDrawContent = true
    private var lastInsets: WindowInsets? = null
    private var useLightStatusBar = true
    private var useLightNavBar: Boolean = false
    protected var containerView: ViewGroup? = null
    private val applyTopPadding = true
    private var applyBottomPadding = true
    private var dialogTitle: CharSequence? = null
    private var titleView: TextView? = null
    private var customView: View? = null
    private var dismissed = false
    private var delegate: BottomSheetDelegateInterface? = null
    private val dismissRunnable = Runnable { dismiss() }
    private var calcMandatoryInsets = false
    private var dimBehind = true
    private val onHideListener: DialogInterface.OnDismissListener? = null
    private var allowCustomAnimation = true
    protected var currentSheetAnimationType = 0
    protected var currentSheetAnimation: AnimatorSet? = null
    private var useFastDismiss = false
    private val canDismissWithSwipe = true
    private val useHardwareLayer = true
    private val dimBehindAlpha = 51
    private var openInterpolator: Interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
    private val onClickListener: DialogInterface.OnClickListener? = null
    private var bottomInset = 0
    private var leftInset = 0
    private var rightInset = 0
    protected var drawNavigationBar = false

    // colors
    private val colorFFFF = 0xffffffff.toInt()
    val color0000 = 0xff000000.toInt()

    private var backDrawable: ColorDrawable = object : ColorDrawable(color0000) {
        override fun setAlpha(alpha: Int) {
            super.setAlpha(alpha)
            container?.invalidate()
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= 30) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        } else {
            window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }

        val vc = ViewConfiguration.get(context)
        touchSlop = vc.scaledTouchSlop

        val padding = Rect()
        shadowDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.sheet_shadow_round, null)?.mutate()
        shadowDrawable?.colorFilter = PorterDuffColorFilter(colorFFFF, PorterDuff.Mode.MULTIPLY)
        shadowDrawable?.getPadding(padding)
        backgroundPaddingLeft = padding.left
        backgroundPaddingTop = padding.top

        container = object: ContainerView(context) {
            override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
                try {
                    return allowDrawContent && super.drawChild(canvas, child, drawingTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        }

        container?.setBackgroundDrawable(backDrawable)

        container?.fitsSystemWindows = true
        container?.setOnApplyWindowInsetsListener(View.OnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            lastInsets = insets
            v.requestLayout()
            if (Build.VERSION.SDK_INT >= 30) {
                return@OnApplyWindowInsetsListener WindowInsets.CONSUMED
            } else {
                return@OnApplyWindowInsetsListener insets.consumeSystemWindowInsets()
            }
        })

        if (Build.VERSION.SDK_INT >= 30) {
            container?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            container?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        backDrawable.alpha = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window = window

        if (Build.VERSION.SDK_INT >= 30) {
            window?.setDecorFitsSystemWindows(true)
        }

        window?.setWindowAnimations(R.style.DialogNoAnimation)
        setContentView(container!!, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        if (useLightStatusBar && Build.VERSION.SDK_INT >= 23) {
            val color = -0xad825d // check this condition
            if (color == -0x1) {// check this condition
                var flags: Int = container?.systemUiVisibility ?: 0
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                container?.systemUiVisibility = flags
            }
        }

        if (useLightNavBar && Build.VERSION.SDK_INT >= 26) {
            AndroidUtilities.setLightNavigationBar(window!!, false)
        }

        if (containerView == null) {
            containerView = object : FrameLayout(context) {
                override fun hasOverlappingRendering(): Boolean {
                    return false
                }

            }
            containerView?.setBackgroundDrawable(shadowDrawable)
            containerView?.setPadding(
                backgroundPaddingLeft,
                (if (applyTopPadding) AndroidUtilities.dp(8f) else 0) + backgroundPaddingTop - 1,
                backgroundPaddingLeft,
                if (applyBottomPadding) AndroidUtilities.dp(8f) else 0
            )
        }

        containerView?.visibility = View.INVISIBLE

        container?.addView(
            containerView,
            0,
            createFrame(
                MATCH_PARENT,
                WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        var topOffset = 0

        if (dialogTitle != null) {
            titleView = TextView(context)
            titleView?.let { titleView ->
                titleView.setLines(1)
                titleView.isSingleLine = true
                titleView.text = dialogTitle

                titleView.setTextColor(-0x8a8a8b)
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                titleView.setPadding(
                    AndroidUtilities.dp(16f),
                    0,
                    AndroidUtilities.dp(16f),
                    AndroidUtilities.dp(8f)
                )

                titleView.ellipsize = TextUtils.TruncateAt.MIDDLE
                titleView.gravity = Gravity.CENTER_VERTICAL
                containerView?.addView(titleView, createFrame(MATCH_PARENT, 48f))
                titleView.setOnTouchListener { v: View?, event: MotionEvent? -> true }
                topOffset += 48
            }
        }

        if (customView != null) {
            if (customView?.parent != null) {
                val viewGroup = customView?.parent as ViewGroup
                viewGroup.removeView(customView)
            }
            containerView?.addView(
                customView,
                createFrame(
                    MATCH_PARENT,
                    (WRAP_CONTENT).toFloat(),
                    Gravity.START or Gravity.TOP,
                    0f,
                    (topOffset).toFloat(),
                    0f,
                    0f
                )
            )
        }

        val windowParams = window?.attributes
        windowParams?.let { params ->
            params.width = MATCH_PARENT
            params.gravity = Gravity.TOP or Gravity.START
            params.dimAmount = 0f
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

            params.height = MATCH_PARENT;
            if (Build.VERSION.SDK_INT >= 28) {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.attributes = params
        }
    }

    override fun show() {
        super.show()

        dismissed = false
        cancelSheetAnimation()
        containerView!!.measure(
            View.MeasureSpec.makeMeasureSpec(
                AndroidUtilities.displaySize.x + backgroundPaddingLeft * 2,
                View.MeasureSpec.AT_MOST
            ),
            View.MeasureSpec.makeMeasureSpec(
                AndroidUtilities.displaySize.y,
                View.MeasureSpec.AT_MOST
            )
        )

        backDrawable.alpha = 0
        containerView?.translationY =
            (AndroidUtilities.statusBarHeight + containerView!!.measuredHeight).toFloat()

        startOpenAnimation()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return if (dismissed) {
            false
        } else super.dispatchTouchEvent(ev)
    }

    override fun dismiss() {
        if (delegate != null && !delegate!!.canDismiss()) {
            return
        }
        if (dismissed) {
            return
        }
        dismissed = true
        onHideListener?.onDismiss(this)
        cancelSheetAnimation()
        var duration: Long = 0
        if (!allowCustomAnimation || !onCustomCloseAnimation()) {
            currentSheetAnimationType = 2
            currentSheetAnimation = AnimatorSet()
            currentSheetAnimation?.playTogether(
                ObjectAnimator.ofFloat(
                    containerView,
                    View.TRANSLATION_Y,
                    (containerView!!.measuredHeight + AndroidUtilities.dp(10f)).toFloat()
                ),
                ObjectAnimator.ofInt(
                    backDrawable,
                    AnimationProperties.COLOR_DRAWABLE_ALPHA,
                    0
                )
            )
            if (useFastDismiss) {
                val height = containerView!!.measuredHeight
                duration = Math.max(
                    60,
                    (250 * (height - containerView!!.translationY) / height.toFloat()).toInt()
                ).toLong()
                currentSheetAnimation?.duration = duration
                useFastDismiss = false
            } else {
                currentSheetAnimation?.duration = 250L
            }
            currentSheetAnimation?.interpolator = CubicBezierInterpolator.DEFAULT
            currentSheetAnimation?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (currentSheetAnimation != null && currentSheetAnimation == animation) {
                        currentSheetAnimation = null
                        currentSheetAnimationType = 0
                        dismissInternal()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (currentSheetAnimation != null && currentSheetAnimation == animation) {
                        currentSheetAnimation = null
                        currentSheetAnimationType = 0
                    }
                }
            })
            currentSheetAnimation?.start()
        }
    }

    fun setBackgroundColor(color: Int) {
        shadowDrawable?.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }

    internal fun canDismissWithSwipe(): Boolean {
        return canDismissWithSwipe
    }

    protected fun onContainerTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    /*fun setTitle(value: CharSequence) {
        setTitle(value, false)
    }*/

    fun setTitle(value: CharSequence, big: Boolean) {
        dialogTitle = value
    }

    protected fun onCustomMeasure(view: View?, width: Int, height: Int): Boolean {
        return false
    }

    protected fun canDismissWithTouchOutside(): Boolean {
        return true
    }

    private fun cancelSheetAnimation() {
        if (currentSheetAnimation != null) {
            currentSheetAnimation?.cancel()
            currentSheetAnimation = null
            currentSheetAnimationType = 0
        }
    }

    private fun startOpenAnimation() {
        if (dismissed) {
            return
        }
        containerView!!.visibility = View.VISIBLE
        if (!onCustomOpenAnimation()) {
            if (useHardwareLayer) {
                container?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
            containerView!!.translationY = containerView!!.measuredHeight.toFloat()
            currentSheetAnimationType = 1
            currentSheetAnimation = AnimatorSet()
            currentSheetAnimation?.let { currentSheetAnimationValue ->
                currentSheetAnimationValue.playTogether(
                    ObjectAnimator.ofFloat(containerView, View.TRANSLATION_Y, 0f),
                    ObjectAnimator.ofInt(
                        backDrawable,
                        AnimationProperties.COLOR_DRAWABLE_ALPHA,
                        if (dimBehind) dimBehindAlpha else 0
                    )
                )
                currentSheetAnimationValue.duration = 400
                currentSheetAnimationValue.startDelay = 20
                currentSheetAnimationValue.interpolator = openInterpolator
                currentSheetAnimationValue.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (currentSheetAnimation == animation) {
                            currentSheetAnimation = null
                            currentSheetAnimationType = 0
                            if (delegate != null) {
                                delegate?.onOpenAnimationEnd()
                            }
                            if (useHardwareLayer) {
                                container?.setLayerType(View.LAYER_TYPE_NONE, null)
                            }
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        if (currentSheetAnimation != null && currentSheetAnimation == animation) {
                            currentSheetAnimation = null
                            currentSheetAnimationType = 0
                        }
                    }
                })
                currentSheetAnimationValue.start()
            }
        }
    }

    fun dismissWithButtonClick(item: Int) {
        if (dismissed) {
            return
        }
        dismissed = true
        cancelSheetAnimation()
        currentSheetAnimationType = 2
        currentSheetAnimation = AnimatorSet()
        currentSheetAnimation?.playTogether(
            ObjectAnimator.ofFloat(
                containerView,
                View.TRANSLATION_Y,
                (containerView!!.measuredHeight + AndroidUtilities.dp(10f)).toFloat()
            ),
            ObjectAnimator.ofInt(
                backDrawable,
                AnimationProperties.COLOR_DRAWABLE_ALPHA,
                0
            )
        )
        currentSheetAnimation?.duration = 180
        currentSheetAnimation?.interpolator = CubicBezierInterpolator.EASE_OUT
        currentSheetAnimation?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (currentSheetAnimation != null && currentSheetAnimation == animation) {
                    currentSheetAnimation = null
                    currentSheetAnimationType = 0
                    onClickListener?.onClick(this@BottomSheet, item)
                    super@BottomSheet.dismiss()
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                if (currentSheetAnimation != null && currentSheetAnimation == animation) {
                    currentSheetAnimation = null
                    currentSheetAnimationType = 0
                }
            }
        })
        currentSheetAnimation?.start()
    }

    fun setDelegate(bottomSheetDelegate: BottomSheetDelegateInterface) {
        delegate = bottomSheetDelegate
    }

    fun dismissInternal() {
        try {
            super.dismiss()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            //FileLog.e(e);
        }
    }

    protected fun onCustomCloseAnimation(): Boolean {
        return false
    }

    protected fun onCustomOpenAnimation(): Boolean {
        return false
    }

    class Builder(context: Context, needFocus: Boolean) {
        private val bottomSheet: BottomSheet = BottomSheet(context)

        fun setCustomView(view: View?): Builder {
            bottomSheet.customView = view
            return this
        }

        fun setTitle(title: CharSequence?, big: Boolean): Builder {
            bottomSheet.dialogTitle = title
            return this
        }

        fun create(): BottomSheet {
            return bottomSheet
        }

        fun setDimBehind(value: Boolean): BottomSheet {
            bottomSheet.dimBehind = value
            return bottomSheet
        }

        fun show(): BottomSheet {
            bottomSheet.show()
            return bottomSheet
        }

        fun setApplyBottomPadding(value: Boolean): Builder {
            bottomSheet.applyBottomPadding = value
            return this
        }

        val dismissRunnable: Runnable
            get() = bottomSheet.dismissRunnable
    }

    interface BottomSheetDelegateInterface {
        fun onOpenAnimationStart()
        fun onOpenAnimationEnd()
        fun canDismiss(): Boolean
    }

    class BottomSheetDelegate : BottomSheetDelegateInterface {
        override fun onOpenAnimationStart() {}
        override fun onOpenAnimationEnd() {}
        override fun canDismiss(): Boolean {
            return true
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun getAdditionalMandatoryOffsets(): Int {
        if (!calcMandatoryInsets) {
            return 0
        }
        val insets = lastInsets?.systemGestureInsets
        return 0
    }

     open inner class ContainerView(context: Context) : FrameLayout(context) {
        private var velocityTracker: VelocityTracker? = null
        private var startedTrackingX = 0
        private var startedTrackingY = 0
        private var startedTrackingPointerId = -1
        private var maybeStartTracking = false
        private var startedTracking = false
        private var currentAnimation: AnimatorSet? = null

        private val rect = Rect()
        val keyboardHeight = 0
        private val backgroundPaint = Paint()
        private var keyboardChanged = false

         init {
             setWillNotDraw(false)
         }

        private fun checkDismiss(velX: Float, velY: Float) {
            val translationY: Float = containerView?.translationY ?: 0f
            val backAnimation = translationY < AndroidUtilities.getPixelsInCM(
                0.8f,
                false
            ) && (velY < 3500 || abs(velY) < abs(velX)) || velY < 0 && abs(velY) >= 3500
            if (!backAnimation) {
                val allowOld: Boolean = allowCustomAnimation
                allowCustomAnimation = false
                useFastDismiss = true
                dismiss()
                allowCustomAnimation = allowOld
            } else {
                currentAnimation = AnimatorSet()
                currentAnimation!!.playTogether(
                    ObjectAnimator.ofFloat(
                        containerView,
                        "translationY",
                        0f
                    )
                )
                currentAnimation!!.setDuration(
                    ((150 * (Math.max(
                        0f,
                        translationY
                    ) / AndroidUtilities.getPixelsInCM(0.8f, false))).toInt()).toLong()
                )
                currentAnimation?.interpolator = CubicBezierInterpolator.EASE_OUT
                currentAnimation?.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (currentAnimation != null && currentAnimation == animation) {
                            currentAnimation = null
                        }
                    }
                })
                currentAnimation!!.start()
            }
        }

        private fun cancelCurrentAnimation() {
            if (currentAnimation != null) {
                currentAnimation!!.cancel()
                currentAnimation = null
            }
        }

        private fun processTouchEvent(ev: MotionEvent?, intercept: Boolean): Boolean {
            if (dismissed) {
                return false
            }
            if (onContainerTouchEvent(ev)) {
                return true
            }
            if (canDismissWithTouchOutside() && ev != null && (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE) && !startedTracking && !maybeStartTracking && ev.pointerCount == 1) {
                startedTrackingX = ev.x.toInt()
                startedTrackingY = ev.y.toInt()
                if (startedTrackingY < containerView?.top ?: 0 || startedTrackingX < containerView?.left ?: 0 || startedTrackingX > containerView?.right ?: 0) {
                    dismiss()
                    return true
                }
                startedTrackingPointerId = ev.getPointerId(0)
                maybeStartTracking = true
                cancelCurrentAnimation()
                if (velocityTracker != null) {
                    velocityTracker!!.clear()
                }
            } else if (ev != null && ev.action == MotionEvent.ACTION_MOVE && ev.getPointerId(0) == startedTrackingPointerId) {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                }
                val dx = Math.abs((ev.x - startedTrackingX).toInt()).toFloat()
                val dy = (ev.y.toInt() - startedTrackingY).toFloat()
                velocityTracker!!.addMovement(ev)

                if (maybeStartTracking && !startedTracking && dy > 0 && dy / 3.0f > Math.abs(dx) && Math.abs(dy) >= touchSlop) {
                    startedTrackingY = ev.y.toInt()
                    maybeStartTracking = false
                    startedTracking = true
                    requestDisallowInterceptTouchEvent(true)
                } else if (startedTracking) {
                    var translationY: Float = containerView?.translationY ?: 0f
                    translationY += dy
                    if (translationY < 0) {
                        translationY = 0f
                    }
                    containerView?.translationY = translationY
                    startedTrackingY = ev.y.toInt()
                    container?.invalidate()
                }
            } else if (ev == null || ev.getPointerId(0) == startedTrackingPointerId && (ev.action == MotionEvent.ACTION_CANCEL || ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_POINTER_UP)) {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain()
                }
                velocityTracker!!.computeCurrentVelocity(1000)
                val translationY: Float = containerView?.translationY ?: 0f
                if (startedTracking || translationY != 0f) {
                    checkDismiss(velocityTracker?.xVelocity ?: 0f, velocityTracker?.yVelocity ?: 0f)
                    startedTracking = false
                } else {
                    maybeStartTracking = false
                    startedTracking = false
                }
                if (velocityTracker != null) {
                    velocityTracker!!.recycle()
                    velocityTracker = null
                }
                startedTrackingPointerId = -1
            }
            return !intercept && maybeStartTracking || startedTracking || !canDismissWithSwipe()
        }

        override fun onTouchEvent(ev: MotionEvent?): Boolean {
            return processTouchEvent(ev, false)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            var width = MeasureSpec.getSize(widthMeasureSpec)
            var height = MeasureSpec.getSize(heightMeasureSpec)
            var containerHeight = height
            getWindowVisibleDisplayFrame(rect)

            if (lastInsets != null) {
                bottomInset = lastInsets?.systemWindowInsetBottom ?: 0
                leftInset = lastInsets?.systemWindowInsetLeft ?: 0
                rightInset = lastInsets?.systemWindowInsetRight ?: 0

                if (Build.VERSION.SDK_INT >= 29) {
                    bottomInset += getAdditionalMandatoryOffsets()
                }

                if (!drawNavigationBar) {
                    containerHeight -= bottomInset
                }
            }

            setMeasuredDimension(width, containerHeight)

            if (lastInsets != null) {
                var inset: Int = lastInsets?.systemWindowInsetBottom ?: 0
                if (Build.VERSION.SDK_INT >= 29) {
                    inset += getAdditionalMandatoryOffsets()
                }
                height -= inset
            }

            if (lastInsets != null) {
                width -= (lastInsets?.systemWindowInsetRight ?: 0) + (lastInsets?.systemWindowInsetLeft ?: 0)
            }

            val isPortrait = width < height

            if (containerView != null) {
                val widthSpec: Int = MeasureSpec.makeMeasureSpec(
                    if (isPortrait) width + backgroundPaddingLeft * 2 else ((width * 0.8f).coerceAtLeast(
                        AndroidUtilities.dp(480f).coerceAtMost(width).toFloat()
                    ).toInt()) + backgroundPaddingLeft * 2, MeasureSpec.EXACTLY
                )

                containerView?.measure(
                    widthSpec,
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
                )
            }
            val childCount = childCount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.visibility == GONE || child === containerView) {
                    continue
                }
                if (!onCustomMeasure(child, width, height)) {
                    measureChildWithMargins(
                        child,
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        0,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
                        0
                    )
                }
            }
        }

         override fun onLayout(changed: Boolean, leftValue: Int, top: Int, rightValue: Int, bottom: Int) {
            var left = leftValue
            var right = rightValue

            if (containerView != null) {
                var t: Int = bottom - top - (containerView?.measuredHeight ?: 0)
                left += lastInsets?.systemWindowInsetLeft ?: 0
                right -= lastInsets?.systemWindowInsetRight ?: 0

                t -= (lastInsets?.systemWindowInsetBottom ?: 0) - if (drawNavigationBar) 0 else bottomInset
                if (Build.VERSION.SDK_INT >= 29) {
                    t -= getAdditionalMandatoryOffsets()
                }
                var l: Int = (right - left - (containerView?.measuredWidth ?: 0)) / 2
                l += lastInsets?.systemWindowInsetLeft ?: 0

                containerView?.layout(
                    l,
                    t,
                    l + (containerView?.measuredWidth ?: 0),
                    t + (containerView?.measuredHeight ?: 0)
                )
            }
            val count = childCount
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (child.visibility == GONE || child === containerView) {
                    continue
                }
            }
            keyboardChanged = false
        }

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            return if (canDismissWithSwipe()) {
                processTouchEvent(event, true)
            } else super.onInterceptTouchEvent(event)
        }

        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            if (maybeStartTracking && !startedTracking) {
                onTouchEvent(null)
            }
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
        }

        override fun hasOverlappingRendering(): Boolean {
            return false
        }

        override fun dispatchDraw(canvas: Canvas) {
            super.dispatchDraw(canvas)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                backgroundPaint.color = Color.LTGRAY
            } else {
                backgroundPaint.color = -0x1000000
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
        }
    }
}