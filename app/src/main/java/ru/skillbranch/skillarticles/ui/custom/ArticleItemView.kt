package ru.skillbranch.skillarticles.ui.custom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import kotlinx.android.extensions.LayoutContainer
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.ArticleItemData
import ru.skillbranch.skillarticles.extensions.attrValue
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.format
import kotlin.math.max

class ArticleItemView constructor(context: Context) : ViewGroup(context), LayoutContainer {

    override val containerView = this

    companion object {
        private const val fontSizeSmall = 12f
        private const val fontSizeMedium = 14f
        private const val fontSizeLarge = 18f
    }

    val tv_date: TextView
    val tv_author: TextView
    val tv_title: TextView
    val iv_poster: ImageView
    val iv_category: ImageView
    val tv_description: TextView
    val iv_likes: ImageView
    val tv_likes_count: TextView
    val iv_comments: ImageView
    val tv_comments_count: TextView
    val tv_read_duration: TextView
    val iv_bookmark: ImageView

    @ColorInt
    private val colorGray: Int = context.getColor(R.color.color_gray)

    @ColorInt
    private val colorPrimary: Int = context.attrValue(R.attr.colorPrimary)

    private val padding: Int = context.dpToIntPx(16)

    private val marginSmall: Int = context.dpToIntPx(8)
    private val marginMedium: Int = context.dpToIntPx(16)
    private val marginLarge: Int = context.dpToIntPx(24)

    private val posterBottomMargin: Int = context.dpToIntPx(20)

    private val posterSize: Int = context.dpToIntPx(64)
    private val categorySize: Int = context.dpToIntPx(40)
    private val cornerRadius: Int = context.dpToIntPx(8)
    private val iconSize: Int = context.dpToIntPx(16)

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(padding)

        tv_date = TextView(context).apply {
            setTextColor(colorGray)
            textSize = fontSizeSmall
        }
        tv_author = TextView(context).apply {
            setTextColor(colorPrimary)
            textSize = fontSizeSmall
        }
        tv_title = TextView(context).apply {
            id = ViewCompat.generateViewId()
            setTextColor(colorPrimary)
            textSize = fontSizeLarge
            setTypeface(typeface, Typeface.BOLD)
        }
        iv_poster = ImageView(context).apply {
            id = ViewCompat.generateViewId()
            layoutParams = LayoutParams(posterSize, posterSize)
        }
        iv_category = ImageView(context).apply {
            layoutParams = LayoutParams(categorySize, categorySize)
        }
        tv_description = TextView(context).apply {
            id = ViewCompat.generateViewId()
            setTextColor(colorGray)
            textSize = fontSizeMedium
        }
        iv_likes = ImageView(context).apply {
            layoutParams = LayoutParams(iconSize, iconSize)
            setImageResource(R.drawable.ic_favorite_black_24dp)
            imageTintList = ColorStateList.valueOf(colorGray)
        }
        tv_likes_count = TextView(context).apply {
            setTextColor(colorGray)
            textSize = fontSizeSmall
        }
        iv_comments = ImageView(context).apply {
            layoutParams = LayoutParams(iconSize, iconSize)
            setImageResource(R.drawable.ic_insert_comment_black_24dp)
            imageTintList = ColorStateList.valueOf(colorGray)
        }
        tv_comments_count = TextView(context).apply {
            setTextColor(colorGray)
            textSize = fontSizeSmall
        }
        tv_read_duration = TextView(context).apply {
            id = ViewCompat.generateViewId()
            setTextColor(colorGray)
            textSize = fontSizeSmall
        }
        iv_bookmark = ImageView(context).apply {
            layoutParams = LayoutParams(iconSize, iconSize)
            setImageResource(R.drawable.bookmark_states)
            imageTintList = ColorStateList.valueOf(colorGray)
        }

    }

    fun bind(data: ArticleItemData) {

        tv_date.text = data.date.format()
        tv_author.text = data.author
        tv_title.text = data.title
        tv_description.text = data.description
        tv_likes_count.text = data.likeCount.toString()
        tv_comments_count.text = data.commentCount.toString()
        tv_read_duration.text = "${data.readDuration} min read"

        addView(tv_date)
        addView(tv_author)
        addView(tv_title)
        addView(tv_description)
        addView(tv_likes_count)
        addView(tv_comments_count)
        addView(tv_read_duration)
        addView(iv_poster)
        addView(iv_category)
        addView(iv_likes)
        addView(iv_comments)
        addView(iv_bookmark)

        Glide.with(context)
            .load(data.poster)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))
            .override(posterSize)
            .into(iv_poster)

        Glide.with(context)
            .load(data.categoryIcon)
            .transform(CenterCrop(), RoundedCorners(cornerRadius))
            .override(categorySize)
            .into(iv_category)

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        var usedHeight = paddingTop

        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val horizontalPadding = paddingStart + paddingEnd

        measureChild(tv_date, widthMeasureSpec, heightMeasureSpec)

        val authorWidth = width - (horizontalPadding + tv_date.measuredWidth)
        val msAuthor = MeasureSpec.makeMeasureSpec(authorWidth, MeasureSpec.EXACTLY)
        tv_author.measure(msAuthor, heightMeasureSpec)

        usedHeight += max(tv_date.measuredHeight, tv_author.measuredHeight)

        val titleWidth =
            width - (horizontalPadding + marginLarge + categorySize / 2 + posterSize)
        val msTitle = MeasureSpec.makeMeasureSpec(titleWidth, MeasureSpec.EXACTLY)
        tv_title.measure(msTitle, heightMeasureSpec)

        usedHeight += max(
            marginSmall + tv_title.measuredHeight + marginSmall,
            marginSmall + posterSize + posterBottomMargin
        )

        usedHeight += marginSmall
        val ms = MeasureSpec.makeMeasureSpec(width - horizontalPadding, MeasureSpec.EXACTLY)
        tv_description.measure(ms, heightMeasureSpec)
        usedHeight += tv_description.measuredHeight

        usedHeight += marginSmall
        measureChild(tv_likes_count, widthMeasureSpec, heightMeasureSpec)
        measureChild(tv_comments_count, widthMeasureSpec, heightMeasureSpec)

        val durationWidth = width - (horizontalPadding + iconSize +
                marginSmall + tv_likes_count.measuredWidth +
                marginMedium + iconSize +
                marginSmall + tv_comments_count.measuredWidth +
                marginMedium + marginMedium + iconSize)
        val msDuration = MeasureSpec.makeMeasureSpec(durationWidth, MeasureSpec.EXACTLY)
        tv_read_duration.measure(msDuration, heightMeasureSpec)

        usedHeight += arrayOf(
            iconSize,
            tv_likes_count.measuredHeight,
            tv_comments_count.measuredHeight,
            tv_read_duration.measuredHeight
        ).max()!!

        usedHeight += paddingBottom
        setMeasuredDimension(width, usedHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        val bodyWidth = r - l - paddingLeft - paddingRight

        var usedHeight = paddingTop
        val left = paddingLeft
        val right = paddingLeft + bodyWidth

        // date & author
        var usedWidth = paddingLeft + tv_date.measuredWidth
        tv_date.layout(left, usedHeight, usedWidth, usedHeight + tv_date.measuredHeight)
        tv_author.layout(
            usedWidth + marginMedium,
            usedHeight,
            right,
            usedHeight + tv_author.measuredHeight
        )
        usedHeight += max(tv_date.measuredHeight, tv_author.measuredHeight)

        // poster & category
        iv_poster.layout(
            right - posterSize,
            usedHeight + marginSmall,
            right,
            usedHeight + marginSmall + posterSize
        )

        iv_category.layout(
            right - posterSize - categorySize / 2,
            usedHeight + marginSmall + posterSize - categorySize / 2,
            right - posterSize + categorySize / 2,
            usedHeight + marginSmall + posterSize + categorySize / 2
        )

        // title
        val titleVerticalMargins = marginSmall + marginSmall
        val secondSectionHeight = max(
            titleVerticalMargins + tv_title.measuredHeight,
            marginSmall + posterSize + posterBottomMargin
        )
        val titleTop = usedHeight +
                marginSmall +
                (secondSectionHeight - titleVerticalMargins) / 2 -
                tv_title.measuredHeight / 2

        tv_title.layout(
            left,
            titleTop,
            left + tv_title.measuredWidth,
            titleTop + tv_title.measuredHeight
        )

        usedHeight += secondSectionHeight

        // description
        usedHeight += marginSmall
        tv_description.layout(left, usedHeight, right, usedHeight + tv_description.measuredHeight)
        usedHeight += tv_description.measuredHeight

        // likes, comments, read duration, bookmark info
        usedHeight += marginSmall
        usedWidth = left

        iv_likes.layout(
            usedWidth,
            usedHeight,
            usedWidth + iconSize,
            usedHeight + iconSize
        )
        usedWidth += iconSize

        tv_likes_count.layout(
            usedWidth + marginSmall,
            usedHeight,
            usedWidth + marginSmall + tv_likes_count.measuredWidth,
            usedHeight + tv_likes_count.measuredHeight
        )
        usedWidth += marginSmall + tv_likes_count.measuredWidth

        iv_comments.layout(
            usedWidth + marginMedium,
            usedHeight,
            usedWidth + marginMedium + iconSize,
            usedHeight + iconSize
        )
        usedWidth += marginMedium + iconSize

        tv_comments_count.layout(
            usedWidth + marginSmall,
            usedHeight,
            usedWidth + marginSmall + tv_comments_count.measuredWidth,
            usedHeight + tv_comments_count.measuredHeight
        )
        usedWidth += marginSmall + tv_comments_count.measuredWidth

        tv_read_duration.layout(
            usedWidth + marginMedium,
            usedHeight,
            usedWidth + marginMedium + tv_read_duration.measuredWidth,
            usedHeight + tv_read_duration.measuredHeight
        )

        iv_bookmark.layout(
            right - iconSize,
            usedHeight,
            right,
            usedHeight + iconSize
        )

    }
}

