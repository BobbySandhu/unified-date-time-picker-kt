/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.text.*
import java.util.*

/**
 *
 * FastDateFormat is a fast and thread-safe version of
 * [java.text.SimpleDateFormat].
 *
 *
 * This class can be used as a direct replacement to
 * `SimpleDateFormat` in most formatting and parsing situations.
 * This class is especially useful in multi-threaded server environments.
 * `SimpleDateFormat` is not thread-safe in any JDK version,
 * nor will it be as Sun have closed the bug/RFE.
 *
 *
 *
 * All patterns are compatible with
 * SimpleDateFormat (except time zones and some year patterns - see below).
 *
 *
 * Since 3.2, FastDateFormat supports parsing as well as printing.
 *
 *
 * Java 1.4 introduced a new pattern letter, `'Z'`, to represent
 * time zones in RFC822 format (eg. `+0800` or `-1100`).
 * This pattern letter can be used here (on all JDK versions).
 *
 *
 * In addition, the pattern `'ZZ'` has been made to represent
 * ISO8601 full format time zones (eg. `+08:00` or `-11:00`).
 * This introduces a minor incompatibility with Java 1.4, but at a gain of
 * useful functionality.
 *
 *
 * Javadoc cites for the year pattern: *For formatting, if the number of
 * pattern letters is 2, the year is truncated to 2 digits; otherwise it is
 * interpreted as a number.* Starting with Java 1.7 a pattern of 'Y' or
 * 'YYY' will be formatted as '2003', while it was '03' in former Java
 * versions. FastDateFormat implements the behavior of Java 7.
 *
 * @since 2.0
 * @version $Id: FastDateFormat.java 1572877 2014-02-28 08:42:25Z britter $
 */
open class FastDateFormat protected constructor(
    pattern: String?,
    timeZone: TimeZone?,
    locale: Locale?,
    centuryStart: Date? = null
) : Format(), DateParser, DatePrinter {
    private val printer: FastDatePrinter
    private val parser: FastDateParser
    // Format methods
    //-----------------------------------------------------------------------
    /**
     *
     * Formats a `Date`, `Calendar` or
     * `Long` (milliseconds) object.
     *
     * @param obj        the object to format
     * @param toAppendTo the buffer to append to
     * @param pos        the position - ignored
     * @return the buffer passed in
     */
    override fun format(obj: Any, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
        return printer.format(obj, toAppendTo, pos)
    }

    /**
     *
     * Formats a millisecond `long` value.
     *
     * @param millis the millisecond value to format
     * @return the formatted string
     * @since 2.1
     */
    override fun format(millis: Long): String {
        return printer.format(millis)
    }

    /**
     *
     * Formats a `Date` object using a `GregorianCalendar`.
     *
     * @param date the date to format
     * @return the formatted string
     */
    override fun format(date: Date): String {
        return printer.format(date)
    }

    /**
     *
     * Formats a `Calendar` object.
     *
     * @param calendar the calendar to format
     * @return the formatted string
     */
    override fun format(calendar: Calendar): String {
        return printer.format(calendar)
    }

    /**
     *
     * Formats a millisecond `long` value into the
     * supplied `StringBuffer`.
     *
     * @param millis the millisecond value to format
     * @param buf    the buffer to format into
     * @return the specified string buffer
     * @since 2.1
     */
    override fun format(millis: Long, buf: StringBuffer): StringBuffer {
        return printer.format(millis, buf)
    }

    /**
     *
     * Formats a `Date` object into the
     * supplied `StringBuffer` using a `GregorianCalendar`.
     *
     * @param date the date to format
     * @param buf  the buffer to format into
     * @return the specified string buffer
     */
    override fun format(date: Date, buf: StringBuffer): StringBuffer {
        return printer.format(date, buf)
    }

    /**
     *
     * Formats a `Calendar` object into the
     * supplied `StringBuffer`.
     *
     * @param calendar the calendar to format
     * @param buf      the buffer to format into
     * @return the specified string buffer
     */
    override fun format(calendar: Calendar, buf: StringBuffer): StringBuffer {
        return printer.format(calendar, buf)
    }

    // Parsing
    //-----------------------------------------------------------------------
    /* (non-Javadoc)
     * @see DateParser#parse(java.lang.String)
     */
    @Throws(ParseException::class)
    override fun parse(source: String): Date {
        return parser.parse(source)
    }

    /* (non-Javadoc)
     * @see DateParser#parse(java.lang.String, java.text.ParsePosition)
     */
    override fun parse(source: String, pos: ParsePosition): Date {
        return parser.parse(source, pos)
    }

    /* (non-Javadoc)
     * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
     */
    override fun parseObject(source: String, pos: ParsePosition): Any {
        return parser.parseObject(source, pos)
    }
    // Accessors
    //-----------------------------------------------------------------------
    /**
     *
     * Gets the pattern used by this formatter.
     *
     * @return the pattern, [java.text.SimpleDateFormat] compatible
     */
    override fun getPattern(): String {
        return printer.pattern
    }

    /**
     *
     * Gets the time zone used by this formatter.
     *
     *
     *
     * This zone is always used for `Date` formatting.
     *
     * @return the time zone
     */
    override fun getTimeZone(): TimeZone {
        return printer.timeZone
    }

    /**
     *
     * Gets the locale used by this formatter.
     *
     * @return the locale
     */
    override fun getLocale(): Locale {
        return printer.locale
    }

    /**
     *
     * Gets an estimate for the maximum string length that the
     * formatter will produce.
     *
     *
     *
     * The actual formatted length will almost always be less than or
     * equal to this amount.
     *
     * @return the maximum formatted length
     */
    val maxLengthEstimate: Int
        get() = printer.maxLengthEstimate
    // Basics
    //-----------------------------------------------------------------------
    /**
     *
     * Compares two objects for equality.
     *
     * @param obj the object to compare to
     * @return `true` if equal
     */
    override fun equals(obj: Any?): Boolean {
        if (obj !is FastDateFormat) {
            return false
        }
        // no need to check parser, as it has same invariants as printer
        return printer == obj.printer
    }

    /**
     *
     * Returns a hashcode compatible with equals.
     *
     * @return a hashcode compatible with equals
     */
    override fun hashCode(): Int {
        return printer.hashCode()
    }

    /**
     *
     * Gets a debugging string version of this formatter.
     *
     * @return a debugging string
     */
    override fun toString(): String {
        return "FastDateFormat[" + printer.pattern + "," + printer.locale + "," + printer.timeZone.id + "]"
    }

    companion object {
        /**
         * Required for serialization support.
         *
         * @see java.io.Serializable
         */
        private const val serialVersionUID = 2L

        /**
         * FULL locale dependent date or time style.
         */
        const val FULL = DateFormat.FULL

        /**
         * LONG locale dependent date or time style.
         */
        const val LONG = DateFormat.LONG

        /**
         * MEDIUM locale dependent date or time style.
         */
        const val MEDIUM = DateFormat.MEDIUM

        /**
         * SHORT locale dependent date or time style.
         */
        const val SHORT = DateFormat.SHORT
        private val cache: FormatCache<FastDateFormat> = object : FormatCache<FastDateFormat>() {
            override fun createInstance(
                pattern: String,
                timeZone: TimeZone,
                locale: Locale
            ): FastDateFormat {
                return FastDateFormat(pattern, timeZone, locale)
            }
        }
        //-----------------------------------------------------------------------
        /**
         *
         * Gets a formatter instance using the default pattern in the
         * default locale.
         *
         * @return a date/time formatter
         */
        val instance: FastDateFormat
            get() = cache.instance

        /**
         *
         * Gets a formatter instance using the specified pattern in the
         * default locale.
         *
         * @param pattern [java.text.SimpleDateFormat] compatible
         * pattern
         * @return a pattern based date/time formatter
         * @throws IllegalArgumentException if pattern is invalid
         */
        fun getInstance(pattern: String?): FastDateFormat {
            return cache.getInstance(pattern, null, null)
        }

        /**
         *
         * Gets a formatter instance using the specified pattern and
         * time zone.
         *
         * @param pattern  [java.text.SimpleDateFormat] compatible
         * pattern
         * @param timeZone optional time zone, overrides time zone of
         * formatted date
         * @return a pattern based date/time formatter
         * @throws IllegalArgumentException if pattern is invalid
         */
        fun getInstance(pattern: String?, timeZone: TimeZone?): FastDateFormat {
            return cache.getInstance(pattern, timeZone, null)
        }

        /**
         *
         * Gets a formatter instance using the specified pattern and
         * locale.
         *
         * @param pattern [java.text.SimpleDateFormat] compatible
         * pattern
         * @param locale  optional locale, overrides system locale
         * @return a pattern based date/time formatter
         * @throws IllegalArgumentException if pattern is invalid
         */
        fun getInstance(pattern: String?, locale: Locale?): FastDateFormat {
            return cache.getInstance(pattern, null, locale)
        }

        /**
         *
         * Gets a formatter instance using the specified pattern, time zone
         * and locale.
         *
         * @param pattern  [java.text.SimpleDateFormat] compatible
         * pattern
         * @param timeZone optional time zone, overrides time zone of
         * formatted date
         * @param locale   optional locale, overrides system locale
         * @return a pattern based date/time formatter
         * @throws IllegalArgumentException if pattern is invalid
         * or `null`
         */
        fun getInstance(pattern: String?, timeZone: TimeZone?, locale: Locale?): FastDateFormat {
            return cache.getInstance(pattern, timeZone, locale)
        }
        //-----------------------------------------------------------------------
        /**
         *
         * Gets a date formatter instance using the specified style in the
         * default time zone and locale.
         *
         * @param style date style: FULL, LONG, MEDIUM, or SHORT
         * @return a localized standard date formatter
         * @throws IllegalArgumentException if the Locale has no date
         * pattern defined
         * @since 2.1
         */
        fun getDateInstance(style: Int): FastDateFormat {
            return cache.getDateInstance(style, null, null)
        }

        /**
         *
         * Gets a date formatter instance using the specified style and
         * locale in the default time zone.
         *
         * @param style  date style: FULL, LONG, MEDIUM, or SHORT
         * @param locale optional locale, overrides system locale
         * @return a localized standard date formatter
         * @throws IllegalArgumentException if the Locale has no date
         * pattern defined
         * @since 2.1
         */
        fun getDateInstance(style: Int, locale: Locale?): FastDateFormat {
            return cache.getDateInstance(style, null, locale)
        }

        /**
         *
         * Gets a date formatter instance using the specified style and
         * time zone in the default locale.
         *
         * @param style    date style: FULL, LONG, MEDIUM, or SHORT
         * @param timeZone optional time zone, overrides time zone of
         * formatted date
         * @return a localized standard date formatter
         * @throws IllegalArgumentException if the Locale has no date
         * pattern defined
         * @since 2.1
         */
        fun getDateInstance(style: Int, timeZone: TimeZone?): FastDateFormat {
            return cache.getDateInstance(style, timeZone, null)
        }

        /**
         *
         * Gets a date formatter instance using the specified style, time
         * zone and locale.
         *
         * @param style    date style: FULL, LONG, MEDIUM, or SHORT
         * @param timeZone optional time zone, overrides time zone of
         * formatted date
         * @param locale   optional locale, overrides system locale
         * @return a localized standard date formatter
         * @throws IllegalArgumentException if the Locale has no date
         * pattern defined
         */
        fun getDateInstance(style: Int, timeZone: TimeZone?, locale: Locale?): FastDateFormat {
            return cache.getDateInstance(style, timeZone, locale)
        }
        //-----------------------------------------------------------------------
        /**
         *
         * Gets a time formatter instance using the specified style in the
         * default time zone and locale.
         *
         * @param style time style: FULL, LONG, MEDIUM, or SHORT
         * @return a localized standard time formatter
         * @throws IllegalArgumentException if the Locale has no time
         * pattern defined
         * @since 2.1
         */
        fun getTimeInstance(style: Int): FastDateFormat {
            return cache.getTimeInstance(style, null, null)
        }

        /**
         *
         * Gets a time formatter instance using the specified style and
         * locale in the default time zone.
         *
         * @param style  time style: FULL, LONG, MEDIUM, or SHORT
         * @param locale optional locale, overrides system locale
         * @return a localized standard time formatter
         * @throws IllegalArgumentException if the Locale has no time
         * pattern defined
         * @since 2.1
         */
        fun getTimeInstance(style: Int, locale: Locale?): FastDateFormat {
            return cache.getTimeInstance(style, null, locale)
        }

        /**
         *
         * Gets a time formatter instance using the specified style and
         * time zone in the default locale.
         *
         * @param style    time style: FULL, LONG, MEDIUM, or SHORT
         * @param timeZone optional time zone, overrides time zone of
         * formatted time
         * @return a localized standard time formatter
         * @throws IllegalArgumentException if the Locale has no time
         * pattern defined
         * @since 2.1
         */
        fun getTimeInstance(style: Int, timeZone: TimeZone?): FastDateFormat {
            return cache.getTimeInstance(style, timeZone, null)
        }

        /**
         *
         * Gets a time formatter instance using the specified style, time
         * zone and locale.
         *
         * @param style    time style: FULL, LONG, MEDIUM, or SHORT
         * @param timeZone optional time zone, overrides time zone of
         * formatted time
         * @param locale   optional locale, overrides system locale
         * @return a localized standard time formatter
         * @throws IllegalArgumentException if the Locale has no time
         * pattern defined
         */
        fun getTimeInstance(style: Int, timeZone: TimeZone?, locale: Locale?): FastDateFormat {
            return cache.getTimeInstance(style, timeZone, locale)
        }
        //-----------------------------------------------------------------------
        /**
         *
         * Gets a date/time formatter instance using the specified style
         * in the default time zone and locale.
         *
         * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
         * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
         * @return a localized standard date/time formatter
         * @throws IllegalArgumentException if the Locale has no date/time
         * pattern defined
         * @since 2.1
         */
        fun getDateTimeInstance(dateStyle: Int, timeStyle: Int): FastDateFormat {
            return cache.getDateTimeInstance(dateStyle, timeStyle, null, null)
        }

        /**
         *
         * Gets a date/time formatter instance using the specified style and
         * locale in the default time zone.
         *
         * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
         * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
         * @param locale    optional locale, overrides system locale
         * @return a localized standard date/time formatter
         * @throws IllegalArgumentException if the Locale has no date/time
         * pattern defined
         * @since 2.1
         */
        fun getDateTimeInstance(dateStyle: Int, timeStyle: Int, locale: Locale?): FastDateFormat {
            return cache.getDateTimeInstance(dateStyle, timeStyle, null, locale)
        }

        /**
         *
         * Gets a date/time formatter instance using the specified style and
         * time zone in the default locale.
         *
         * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
         * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
         * @param timeZone  optional time zone, overrides time zone of
         * formatted date
         * @return a localized standard date/time formatter
         * @throws IllegalArgumentException if the Locale has no date/time
         * pattern defined
         * @since 2.1
         */
        fun getDateTimeInstance(
            dateStyle: Int,
            timeStyle: Int,
            timeZone: TimeZone?
        ): FastDateFormat {
            return getDateTimeInstance(dateStyle, timeStyle, timeZone, null)
        }

        /**
         *
         * Gets a date/time formatter instance using the specified style,
         * time zone and locale.
         *
         * @param dateStyle date style: FULL, LONG, MEDIUM, or SHORT
         * @param timeStyle time style: FULL, LONG, MEDIUM, or SHORT
         * @param timeZone  optional time zone, overrides time zone of
         * formatted date
         * @param locale    optional locale, overrides system locale
         * @return a localized standard date/time formatter
         * @throws IllegalArgumentException if the Locale has no date/time
         * pattern defined
         */
        fun getDateTimeInstance(
            dateStyle: Int, timeStyle: Int, timeZone: TimeZone?, locale: Locale?
        ): FastDateFormat {
            return cache.getDateTimeInstance(dateStyle, timeStyle, timeZone, locale)
        }
    }
    // Constructor
    //-----------------------------------------------------------------------
    /**
     *
     * Constructs a new FastDateFormat.
     *
     * @param pattern      [java.text.SimpleDateFormat] compatible pattern
     * @param timeZone     non-null time zone to use
     * @param locale       non-null locale to use
     * @param centuryStart The start of the 100 year period to use as the "default century" for 2 digit year parsing.  If centuryStart is null, defaults to now - 80 years
     * @throws NullPointerException if pattern, timeZone, or locale is null.
     */
    // Constructor
    //-----------------------------------------------------------------------
    /**
     *
     * Constructs a new FastDateFormat.
     *
     * @param pattern  [java.text.SimpleDateFormat] compatible pattern
     * @param timeZone non-null time zone to use
     * @param locale   non-null locale to use
     * @throws NullPointerException if pattern, timeZone, or locale is null.
     */
    init {
        printer = FastDatePrinter(
            pattern ?: "",
            timeZone ?: TimeZone.getDefault(),
            locale ?: Locale("en")
        )
        parser = FastDateParser(pattern, timeZone, locale, centuryStart)
    }
}