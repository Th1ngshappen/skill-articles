package ru.skillbranch.skillarticles.extensions

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.delegates.AttrValue
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun Context.attrValue(@AttrRes color: Int) : Int {
    val tv = TypedValue()
    return if (this.theme.resolveAttribute(color, tv, true)) tv.data
    else throw Resources.NotFoundException("Resource with id $color is not found")
}

fun Context.dpToPx(dp: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics
    )
}

fun Context.dpToIntPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics
    ).toInt()
}