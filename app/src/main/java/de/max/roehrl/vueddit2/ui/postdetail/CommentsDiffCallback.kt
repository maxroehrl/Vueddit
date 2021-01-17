package de.max.roehrl.vueddit2.ui.postdetail

import androidx.recyclerview.widget.DiffUtil
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

        return item1.id == item2.id
    }
}