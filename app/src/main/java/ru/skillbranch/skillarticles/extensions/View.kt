package ru.skillbranch.skillarticles.extensions

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.navigation.NavDestination
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout

fun View.setMarginOptionally(
    left: Int = marginLeft,
    top: Int = marginTop,
    right: Int = marginRight,
    bottom: Int = marginBottom
) {
    (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(left, top, right, bottom)
}

fun View.setPaddingOptionally(
    left: Int = paddingLeft,
    right: Int = paddingRight,
    top: Int = paddingTop,
    bottom: Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}

fun BottomNavigationView.selectDestination(destination: NavDestination) {
    this.menu.findItem(destination.id)?.let {
        if (!it.isChecked) it.isChecked = true
    }
}

fun TextInputLayout.disableError() {
    isErrorEnabled = false
    error = null
}

fun EditText.getTrimmedString() = text.trim().toString()