package com.xabber.android.ui.text

import android.content.Context
import com.xabber.android.R
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

infix fun Long.isSameDayWith(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { time = Date(this@isSameDayWith) }
    val cal2 = Calendar.getInstance().apply { time = Date(timestamp) }
    return cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR] &&
            cal1[Calendar.YEAR] == cal2[Calendar.YEAR]
}

fun Date.getSmartTimeTextForRoster(): String {
    val dayInMillis = GregorianCalendar.getInstance().apply {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }.timeInMillis

    val hourInMillis = GregorianCalendar.getInstance()
        .apply { add(Calendar.HOUR, -12) }
        .timeInMillis

    val weekInMillis = GregorianCalendar.getInstance()
        .apply { add(Calendar.HOUR, -168) }
        .timeInMillis

    val yearInMillis = GregorianCalendar.getInstance()
        .apply { add(Calendar.YEAR, -1) }
        .timeInMillis

    val formatter = when {
        (yearInMillis > this.time) -> SimpleDateFormat("dd MMM yyyy", Locale.ROOT)

        (weekInMillis > this.time) -> SimpleDateFormat("MMM d", Locale.ROOT)

        (hourInMillis > this.time) -> SimpleDateFormat("E", Locale.ROOT)

        (this.time in (hourInMillis + 1) until dayInMillis) -> {
            SimpleDateFormat("HH:mm:ss", Locale.ROOT)
        }

        (dayInMillis < this.time) -> SimpleDateFormat("HH:mm:ss", Locale.ROOT)

        else -> SimpleDateFormat("dd MM yyyy HH:mm:ss", Locale.ROOT)
    }

    return formatter.format(this)
}

/**
 * @return String with date and time to be display.
 */
fun Date.getDateTimeText(): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(this)


fun getDurationStringForVoiceMessage(current: Long?, duration: Long): String {

    fun Long.timeToString(): String {
        return String.format(
            Locale.getDefault(), "%01d:%02d",
            TimeUnit.SECONDS.toMinutes(this),
            TimeUnit.SECONDS.toSeconds(this) % 60
        )
    }
    return current?.let { "${current.timeToString()} / ${duration.timeToString()}" }
        ?: duration.timeToString()
}

fun Date.isCurrentYear(): Boolean {
    val calendarOne = Calendar.getInstance()
    val calendarTwo = Calendar.getInstance()
    calendarOne.time = this
    calendarTwo.time = Date()
    return calendarOne[Calendar.YEAR] == calendarTwo[Calendar.YEAR]
}

fun Date.isToday(): Boolean {
    val calendarOne = Calendar.getInstance()
    val calendarTwo = Calendar.getInstance()
    calendarOne.time = this
    calendarTwo.time = Date()
    return calendarOne[Calendar.DAY_OF_YEAR] == calendarTwo[Calendar.DAY_OF_YEAR] &&
            calendarOne[Calendar.YEAR] == calendarTwo[Calendar.YEAR]
}

fun Date.isYesterday(): Boolean {
    val calendarOne = Calendar.getInstance()
    val calendarTwo = Calendar.getInstance()
    calendarOne.time = this
    calendarTwo.time = Date()
    return calendarOne[Calendar.DAY_OF_YEAR] == calendarTwo[Calendar.DAY_OF_YEAR] - 1 &&
            calendarOne[Calendar.YEAR] == calendarTwo[Calendar.YEAR]
}

fun Date.getDayOfWeek(locale: Locale = Locale.ROOT): String? {
    val c = Calendar.getInstance().apply { time = this@getDayOfWeek }[Calendar.DAY_OF_WEEK]
    return DateFormatSymbols(locale).weekdays[c]
}


fun Long.getHumanReadableEstimatedTime(context: Context): String {
    require(this >= 0) { "Duration must be greater than zero!" }

    val currentTime = System.currentTimeMillis() / 1000
    var secondsLeft: Long = this - currentTime

    val MILLIS_IN_DAY: Long = 86400
    val MILLIS_IN_HOUR: Long = 3600
    val MILLIS_IN_MINUTE: Long = 60

    val days = secondsLeft / MILLIS_IN_DAY
    secondsLeft -= days * MILLIS_IN_DAY

    val hours = secondsLeft / MILLIS_IN_HOUR
    secondsLeft -= hours * MILLIS_IN_HOUR

    val minutes = (secondsLeft / MILLIS_IN_MINUTE).toInt()

    val resources = context.resources

    return when {
        days >= 1 -> resources.getQuantityString(R.plurals.estimated_in_days, days.toInt(), days)
        hours >= 1 -> resources.getQuantityString(R.plurals.estimated_in_hours, hours.toInt(), hours)
        minutes >= 1 -> resources.getQuantityString(R.plurals.estimated_in_minutes, minutes, minutes)
        secondsLeft >= 1 -> resources.getQuantityString(
            R.plurals.estimated_in_seconds,
            secondsLeft.toInt(),
            secondsLeft
        )
        else -> ""
    }
}