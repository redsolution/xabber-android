package com.xabber.android.ui.fragment.chatListFragment

import androidx.recyclerview.widget.DiffUtil
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.roster.AbstractContact

class ChatItemDiffUtil(private val oldList: List<AbstractContact>,
                       private val newList: List<AbstractContact>,
                       val adapter: ChatListAdapter) :DiffUtil.Callback(){

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItemHolder = adapter.holdersMap[oldItemPosition]
        val newAbstractContact = newList[newItemPosition]
        val newMessageItem = MessageManager.getInstance()
                .getOrCreateChat(newAbstractContact.account, newAbstractContact.user).lastMessage

        if (oldItemHolder == null || newMessageItem == null) return false

        val isMessagesAreEqual = oldItemHolder.messageItem!!.isUiEqual(newMessageItem)
        val isStatusesAreEqual = oldItemHolder.rosterStatus == newAbstractContact.statusMode.statusLevel
        val isAvatarsAreEqual = oldItemHolder.avatarIV.drawable == newAbstractContact.avatar
        //TODO think about colorIndicator comparing

        return isMessagesAreEqual && isStatusesAreEqual && isAvatarsAreEqual
    }
}