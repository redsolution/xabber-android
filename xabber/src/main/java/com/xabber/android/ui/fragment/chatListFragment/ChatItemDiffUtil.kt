package com.xabber.android.ui.fragment.chatListFragment

import androidx.recyclerview.widget.DiffUtil
import com.xabber.android.data.database.realmobjects.ChatRealmObject
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.roster.RosterManager

class ChatItemDiffUtil(private val oldList: List<ChatRealmObject>,
                       private val newList: List<ChatRealmObject>,
                       val adapter: ChatListAdapter) :DiffUtil.Callback(){

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        try {
            val oldItemHolder = adapter.holdersMap[oldItemPosition]
            val newAbstractContact = newList[newItemPosition]

            if (oldItemHolder == null) return false

            val isMessagesAreEqual = oldItemHolder.messageRealmObject?.isUiEqual(newAbstractContact.lastMessage)
            //val isStatusesAreEqual = oldItemHolder.rosterStatus == newAbstractContact.statusMode.statusLevel
            val isUnreadCountAreEqual = oldItemHolder.unreadCountTV.text == MessageManager.getInstance()
                    .getOrCreateChat(newAbstractContact.accountJid, newAbstractContact.contactJid).unreadMessageCount.toString()
            val isAvatarsAreEqual = oldItemHolder.avatarIV.drawable == RosterManager.getInstance()
                    .getAbstractContact(newAbstractContact.accountJid, newAbstractContact.contactJid)
                    .getAvatar(true)
            //val isColorIndicatorsAreEqual = oldItemHolder.accountColorIndicator == ColorManager
            //       .getInstance().accountPainter.getAccountMainColor(newAbstractContact.account)
            val isTextEqual = true //oldItemHolder.messageTextTV.text == newMessageItem.text

            return isMessagesAreEqual!! && isAvatarsAreEqual && isTextEqual
                    && isUnreadCountAreEqual
        } catch (e: Exception) { LogManager.exception("ChatItemViewHolder", e)}

        return false

//        return isMessagesAreEqual && isStatusesAreEqual && isAvatarsAreEqual
//                && isColorIndicatorsAreEqual && isTextEqual
    }
}