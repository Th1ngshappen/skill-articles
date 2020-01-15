package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import ru.skillbranch.skillarticles.ui.custom.ArticleSubmenu

class SubmenuBehavior : CoordinatorLayout.Behavior<ArticleSubmenu> {

    companion object {
        private const val STATE_SCROLLED_NONE = 0
        private const val STATE_SCROLLED_DOWN = 1
        private const val STATE_SCROLLED_UP = 2
    }

    private var currentState = STATE_SCROLLED_NONE

    constructor() : super()

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs)

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: ArticleSubmenu,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: ArticleSubmenu,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        if (dyConsumed > 0) {
            slideDown(child)
        } else if (dyConsumed < 0) {
            slideUp(child)
        }
    }

    private fun slideUp(submenu: ArticleSubmenu) {
        if (currentState == STATE_SCROLLED_UP) return
        currentState = STATE_SCROLLED_UP

        if (submenu.isOpen.not()) return

    }

    private fun slideDown(submenu: ArticleSubmenu) {
        if (currentState == STATE_SCROLLED_DOWN) return
        currentState = STATE_SCROLLED_DOWN

        if (submenu.isOpen.not()) return
    }
}