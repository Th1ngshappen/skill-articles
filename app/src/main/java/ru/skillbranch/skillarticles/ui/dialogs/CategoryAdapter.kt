package ru.skillbranch.skillarticles.ui.dialogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_category_dialog.*
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.data.local.entities.CategoryData

class CategoryAdapter(private val listener: (String, Boolean) -> Unit) :
    ListAdapter<CategoryDataItem, CategoryVH>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        val inflater = LayoutInflater.from(parent.context)
        val containerView = inflater.inflate(R.layout.item_category_dialog, parent, false)
        return CategoryVH(containerView, listener)
    }

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        holder.bind(getItem(position))
    }
}

class CategoryVH(override val containerView: View, val listener: (String, Boolean) -> Unit) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(item: CategoryDataItem) {

        // remove listener
        ch_select.setOnCheckedChangeListener(null)

        // bind data
        ch_select.isChecked = item.isChecked
        with(containerView.context) {
            Glide.with(this)
                .load(item.icon)
                .apply(RequestOptions.circleCropTransform())
                .override(iv_icon.width)
                .into(iv_icon)
        }
        tv_category.text = item.title
        tv_count.text = item.articlesCount.toString()

        // set listeners
        ch_select.setOnCheckedChangeListener { _, isChecked -> listener.invoke(item.id, isChecked) }
        itemView.setOnClickListener { ch_select.toggle() }
    }
}

class CategoryDiffCallback() : DiffUtil.ItemCallback<CategoryDataItem>() {
    override fun areItemsTheSame(oldItem: CategoryDataItem, newItem: CategoryDataItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: CategoryDataItem, newItem: CategoryDataItem): Boolean =
        oldItem == newItem
}

data class CategoryDataItem(
    val id: String,
    val icon: String,
    val title: String,
    val articlesCount: Int,
    var isChecked: Boolean
)

fun CategoryData.toItem(checked: Boolean = false) =
    CategoryDataItem(categoryId, icon, title, articlesCount, checked)