package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import ru.skillbranch.skillarticles.ui.custom.Bottombar

// 2018 https://medium.com/@zoha131/coordinatorlayout-behavior-basic-fd9c10d3c6e3
// 2016 https://habr.com/ru/post/277813/

// app:layout_behavior=".ui.custom.behaviors.BottombarBehavior"

class BottombarBehavior : CoordinatorLayout.Behavior<Bottombar> {

    constructor() : super()

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs)

}