package com.xabber.android.ui.fragment.chatListFragment

import androidx.recyclerview.widget.DiffUtil
import com.xabber.android.data.message.chat.AbstractChat

class ChatItemDiffUtil(
    private val oldList: List<AbstractChat>,
    private val newList: List<AbstractChat>,
    val adapter: ChatListAdapter
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        ChatListItemData.createFromChat(
            oldList[oldItemPosition], adapter.recyclerView.context
        ) == ChatListItemData.createFromChat(
            newList[newItemPosition], adapter.recyclerView.context
        )

}