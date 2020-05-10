package ru.skillbranch.skillarticles.extensions

import java.text.SimpleDateFormat
import java.util.*
import ru.skillbranch.skillarticles.extensions.TimeUnits.*

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
    val day1 = this.time / TimeUnits.DAY.value
    val day2 = date.time / TimeUnits.DAY.value
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
