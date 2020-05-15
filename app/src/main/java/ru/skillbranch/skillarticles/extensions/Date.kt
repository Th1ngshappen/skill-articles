package ru.skillbranch.skillarticles.extensions

import java.text.SimpleDateFormat
import java.util.*
import ru.skillbranch.skillarticles.extensions.TimeUnits.*
import kotlin.math.abs

fun Date.format(pattern: String = "HH:mm:ss dd.MM.yy"): String {
    val dateFormat = SimpleDateFormat(pattern, Locale("ru"))
    return dateFormat.format(this)
}

fun Date.shortFormat(): String {
    val pattern = if (this.isSameDay(Date())) "HH:mm" else "dd.MM.yy"
    val dateFormat = SimpleDateFormat(pattern, Locale("ru"))
    return dateFormat.format(this)
}

fun Date.isSameDay(date: Date): Boolean {
    val day1 = this.time / DAY.value
    val day2 = date.time / DAY.value
    return day1 == day2
}

fun Date.add(value: Int, units: TimeUnits = SECOND): Date {
    this.time += units.value * value
    return this
}

fun getUnitForm(value: Int, unit: TimeUnits): String {

    val triple = when (unit) {
        SECOND -> Triple("секунд", "секунду", "секунды")
        MINUTE -> Triple("минут", "минуту", "минуты")
        HOUR -> Triple("часов", "час", "часа")
        DAY -> Triple("дней", "день", "дня")
    }

    // 21-24 час(а), 31-34 час(а), но 11-14 часов
    val i = value % 100
    return when (i) {
        0, in 5..19 -> triple.first
        1 -> triple.second
        in 2..4 -> triple.third
        else -> getUnitForm(i % 10, unit)
    }
}

enum class TimeUnits(val value: Long) {
    SECOND(1000L),
    MINUTE(60 * SECOND.value),
    HOUR(60 * MINUTE.value),
    DAY(24 * HOUR.value);

    fun plural(value: Int) = "$value ${getUnitForm(value, this)}"
}

fun getTenseForm(interval: String, isPast: Boolean): String {
    return if (isPast) "$interval назад" else "через $interval"
}

fun Date.humanizeDiff(date: Date = Date()): String {
    val isPast = this.time < date.time
    val diff = abs(date.time - this.time)

    return when {
        diff <= SECOND.value -> "только что"
        diff <= 45 * SECOND.value -> getTenseForm("несколько секунд", isPast)
        diff <= 75 * SECOND.value -> getTenseForm("минуту", isPast)
        diff <= 45 * MINUTE.value -> getTenseForm(
            MINUTE.plural((diff / MINUTE.value).toInt()),
            isPast
        )
        diff <= 75 * MINUTE.value -> getTenseForm("час", isPast)
        diff <= 22 * HOUR.value -> getTenseForm(HOUR.plural((diff / HOUR.value).toInt()), isPast)
        diff <= 26 * HOUR.value -> getTenseForm("день", isPast)
        diff <= 360 * DAY.value -> getTenseForm(DAY.plural((diff / DAY.value).toInt()), isPast)
        else -> if (isPast) "более года назад" else "более чем через год"
    }
}