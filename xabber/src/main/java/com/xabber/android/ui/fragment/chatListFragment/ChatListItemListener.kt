package com.xabber.android.ui.fragment.chatListFragment

import android.view.ContextMenu
import android.view.View
import com.xabber.android.data.message.chat.AbstractChat

interface ChatListItemListener: View.OnCreateContextMenuListener{
    fun onChatItemClick(contact: AbstractChat)
    fun onChatAvatarClick(contact: AbstractChat)
    fun onChatItemContextMenu(menu: ContextMenu, contact: AbstractChat)
    fun onChatItemSwiped(abstractContact: AbstractChat)
    fun onListBecomeEmpty()
}