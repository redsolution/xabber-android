package com.xabber.android.ui.fragment.chatListFragment

import androidx.recyclerview.widget.DiffUtil

class ChatItemDiffUtil(private val oldList: List<ChatItemVO>,
                       private val newList: List<ChatItemVO>,
                       val adapter: ChatListAdapter) :DiffUtil.Callback(){

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldChatItemVO = oldList[oldItemPosition]
        val newChatItemVO = newList[newItemPosition]
        return oldChatItemVO.accountColorIndicator == newChatItemVO.accountColorIndicator
                && oldChatItemVO.contactAvatarDrawable == newChatItemVO.contactAvatarDrawable
                && oldChatItemVO.contactStatusLevel == newChatItemVO.contactStatusLevel
                && oldChatItemVO.contactName == newChatItemVO.contactName
                && oldChatItemVO.notificationMuteIcon == newChatItemVO.notificationMuteIcon
                && oldChatItemVO.unreadCount == newChatItemVO.unreadCount
                && oldChatItemVO.lastMessageTime == newChatItemVO.lastMessageTime
                && oldChatItemVO.messageText == newChatItemVO.messageText
                && oldChatItemVO.messageStatusDrawable == newChatItemVO.messageStatusDrawable
    }
}