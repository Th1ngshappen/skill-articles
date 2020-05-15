package ru.skillbranch.skillarticles.ui.article

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import ru.skillbranch.skillarticles.data.models.CommentItemData
import ru.skillbranch.skillarticles.ui.custom.CommentItemView

class CommentsAdapter(private val listener: (CommentItemData) -> Unit) :
    PagedListAdapter<CommentItemData, CommentVH>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH =
        CommentVH(CommentItemView(parent.context), listener)

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        holder.bind(getItem(position))
    }

}

// 8: 01:15:50
// RecyclerView будет находиться внутри article и, соответственно, это будет не просто список
// фиксированной величины, а это будет RV, где каждый VH будет соответствовать элементу списка,
// т.е. он будет непрокручиваемым, поэтому имеет смысл сделать всё это как кастомное вью?
// в том числе потому что там есть уровень вложенности
// это место самое тонкое, потому что каждый VH соответствует элементу списка и если создавать VH
// из разметки, пришлось бы создавать 5 xml на 5 состояний трэда
class CommentVH(override val containerView: View, val listener: (CommentItemData) -> Unit) :
    RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(item: CommentItemData?) {
        (containerView as CommentItemView).bind(item)
        if (item != null) itemView.setOnClickListener { listener(item) }
    }
}

class CommentDiffCallback() : DiffUtil.ItemCallback<CommentItemData>() {
    override fun areItemsTheSame(oldItem: CommentItemData, newItem: CommentItemData): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: CommentItemData, newItem: CommentItemData): Boolean =
        oldItem == newItem
}