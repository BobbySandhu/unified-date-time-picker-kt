package com.bobgenix.datetimedialogkt

import android.graphics.drawable.ColorDrawable
import android.util.Property

internal object AnimationProperties {

    val COLOR_DRAWABLE_ALPHA: Property<ColorDrawable, Int> =
        object : IntProperty<ColorDrawable>("alpha") {
            override fun setValue(`object`: ColorDrawable, value: Int) {
                `object`.alpha = value
            }

            override fun get(`object`: ColorDrawable): Int {
                return `object`.alpha
            }
        }

    abstract class IntProperty<T>(name: String?) : Property<T, Int>(
        Int::class.java, name
    ) {
        abstract fun setValue(`object`: T, value: Int)
        override fun set(`object`: T, value: Int) {
            setValue(`object`, value)
        }
    }
}