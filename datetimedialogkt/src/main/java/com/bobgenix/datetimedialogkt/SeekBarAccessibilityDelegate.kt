package com.bobgenix.datetimedialogkt

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.ViewCompat
import android.view.accessibility.AccessibilityEvent
import com.bobgenix.datetimedialogkt.SeekBarAccessibilityDelegate
import android.text.TextUtils
import android.os.Build
import android.view.View
import android.widget.SeekBar
import java.util.HashMap

abstract class SeekBarAccessibilityDelegate : View.AccessibilityDelegate() {

    private val accessibilityEventRunnables: MutableMap<View, Runnable> = HashMap(4)

    private val onAttachStateChangeListener: View.OnAttachStateChangeListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                v.removeCallbacks(accessibilityEventRunnables.remove(v))
                v.removeOnAttachStateChangeListener(this)
            }
        }

    override fun performAccessibilityAction(host: View, action: Int, args: Bundle): Boolean {
        return if (super.performAccessibilityAction(host, action, args)) {
            true
        } else performAccessibilityActionInternal(host, action, args)
    }

    private fun performAccessibilityActionInternal(host: View?, action: Int, args: Bundle?): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
            doScroll(host, action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            host?.let { postAccessibilityEventRunnable(it) }
            return true
        }
        return false
    }

    private fun postAccessibilityEventRunnable(host: View) {
        if (!ViewCompat.isAttachedToWindow(host)) {
            return
        }
        var runnable = accessibilityEventRunnables[host]
        if (runnable == null) {
            accessibilityEventRunnables[host] =
                Runnable { sendAccessibilityEvent(host, AccessibilityEvent.TYPE_VIEW_SELECTED) }
                    .also { runnable = it }
            host.addOnAttachStateChangeListener(onAttachStateChangeListener)
        } else {
            host.removeCallbacks(runnable)
        }
        host.postDelayed(runnable, 400)
    }

    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        onInitializeAccessibilityNodeInfoInternal(host, info)
    }

    private fun onInitializeAccessibilityNodeInfoInternal(host: View?, info: AccessibilityNodeInfo) {
        info.className = SEEK_BAR_CLASS_NAME
        val contentDescription = getContentDescription(host)

        if (!TextUtils.isEmpty(contentDescription)) {
            info.text = contentDescription
        }

        if (canScrollBackward(host)) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        }

        if (canScrollForward(host)) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        }
    }

    protected open fun getContentDescription(host: View?): CharSequence? {
        return null
    }

    protected abstract fun doScroll(host: View?, backward: Boolean)
    protected abstract fun canScrollBackward(host: View?): Boolean
    protected abstract fun canScrollForward(host: View?): Boolean

    companion object {
        private val SEEK_BAR_CLASS_NAME: CharSequence = SeekBar::class.java.name
    }
}