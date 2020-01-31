package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import ru.skillbranch.skillarticles.ui.custom.Bottombar

// 2018 https://medium.com/@zoha131/coordinatorlayout-behavior-basic-fd9c10d3c6e3
// 2016 https://habr.com/ru/post/277813/

// app:layout_behavior=".ui.custom.behaviors.BottombarBehavior"

class BottombarBehavior : CoordinatorLayout.Behavior<Bottombar>() {

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: Bottombar,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: Bottombar,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        // dy < 0 scroll down
        // dy > 0 scroll up
        if (!child.isSearchMode) {
            val offset = MathUtils.clamp(child.translationY + dy, 0f, child.height.toFloat())
            if (offset != child.translationY) child.translationY = offset
        }
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
    }

}