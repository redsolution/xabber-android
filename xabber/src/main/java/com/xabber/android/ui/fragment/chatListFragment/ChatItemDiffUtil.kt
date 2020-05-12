package com.xabber.android.ui.fragment.chatListFragment

import androidx.recyclerview.widget.DiffUtil
import com.xabber.android.data.message.chat.AbstractChat

class ChatItemDiffUtil(private val oldList: List<AbstractChat>,
                       private val newList: List<AbstractChat>,
                       val adapter: ChatListAdapter) :DiffUtil.Callback(){

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
            //oldList[oldItemPosition] == newList[newItemPosition]

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//        try {
//            val oldItemHolder = adapter.holdersMap[oldItemPosition]
//            val newAbstractContact = newList[newItemPosition]
//
//            if (oldItemHolder == null) return false
//
//            val vCardName = VCardManager.getInstance().getName(newAbstractContact.contactJid.bareJid)
//
//            val isNamesArEqual = if (vCardName.equals("")) oldItemHolder.contactNameTV.text == newAbstractContact.stringContactJid
//                else oldItemHolder.contactNameTV.text == vCardName
//
//            val isMessagesAreEqual = oldItemHolder.messageRealmObject?.isUiEqual(newAbstractContact.lastMessage)
//            //val isStatusesAreEqual = oldItemHolder.rosterStatus == newAbstractContact.statusMode.statusLevel
//            val isUnreadCountAreEqual = oldItemHolder.unreadCountTV.text == MessageManager.getInstance()
//                    .getOrCreateChat(newAbstractContact.accountJid, newAbstractContact.contactJid).unreadMessageCount.toString()
//            val isAvatarsAreEqual = oldItemHolder.avatarIV.drawable == RosterManager.getInstance()
//                    .getAbstractContact(newAbstractContact.accountJid, newAbstractContact.contactJid)
//                    .getAvatar(true)
//            //val isColorIndicatorsAreEqual = oldItemHolder.accountColorIndicator == ColorManager
//            //       .getInstance().accountPainter.getAccountMainColor(newAbstractContact.account)
//            val isTextEqual = true //oldItemHolder.messageTextTV.text == newMessageItem.text
//
//            return isMessagesAreEqual!! && isAvatarsAreEqual && isTextEqual
//                    && isUnreadCountAreEqual && isNamesArEqual
//        } catch (e: Exception) { LogManager.exception("ChatItemViewHolder", e)}

        return false

//        return isMessagesAreEqual && isStatusesAreEqual && isAvatarsAreEqual
//                && isColorIndicatorsAreEqual && isTextEqual
    }
}