package ru.skillbranch.skillarticles.ui.custom.spans

import android.graphics.Color
import android.text.style.ForegroundColorSpan

// 56:22
// Color.WHITE, потому что и для светлой темы, и для тёмной у нас будет всегда один и тот же цвет
open class SearchSpan : ForegroundColorSpan(Color.WHITE)