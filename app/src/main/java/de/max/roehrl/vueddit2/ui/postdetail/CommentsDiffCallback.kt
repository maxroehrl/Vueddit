package de.max.roehrl.vueddit2.ui.postdetail

import androidx.recyclerview.widget.DiffUtil
import de.max.roehrl.vueddit2.model.Comment
import de.max.roehrl.vueddit2.model.NamedItem

class CommentsDiffCallback(private val oldList: List<NamedItem>, private val newList: List<NamedItem>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val item1 = oldList[oldPosition]
        val item2 = newList[newPosition]

        return if (item1 is Comment && item2 is Comment) {
            item1.isLoading == item2.isLoading && item1.isCollapsed() == item2.isCollapsed()
        } else {
            true
        }
    }
}