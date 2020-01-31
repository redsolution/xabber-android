package com.xabber.android.ui.fragment.chatListFragment

import androidx.recyclerview.widget.DiffUtil
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.roster.AbstractContact

class ChatItemDiffUtil(private val oldList: List<AbstractContact>, private val newList: List<AbstractContact>, val adapter: ChatListAdapter) :DiffUtil.Callback(){
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val view = adapter.recyclerView.getChildAt(oldItemPosition)

        val oldViewHolder = if (view != null) adapter.recyclerView.getChildViewHolder(view) as ChatViewHolder else return false
        val newContact = newList[newItemPosition]
        val newMessage = MessageManager.getInstance()
                .getOrCreateChat(newContact.account, newContact.user)
                .lastMessage
        val isTextMessageEqual = oldViewHolder.messageTextTV.text == newMessage?.text
        val isAvatarEqual = oldViewHolder.avatarIV.drawable == newContact.avatar
        val isPresenceEqual = oldViewHolder.statusLevel == newContact.statusMode.statusLevel
        val isContactNameEqual = oldViewHolder.contactNameTV.text == newContact.name
        return  isAvatarEqual && isTextMessageEqual && isPresenceEqual && isContactNameEqual
    }
}