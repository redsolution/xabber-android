package com.xabber.android.ui.fragment.chatListFragment

import android.view.ContextMenu
import android.view.View
import com.xabber.android.data.roster.AbstractContact

interface ChatListItemListener: View.OnCreateContextMenuListener{
    fun onChatItemClick(contact: AbstractContact)
    fun onChatAvatarClick(contact: AbstractContact)
    fun onChatItemContextMenu(menu: ContextMenu, contact: AbstractContact)
    fun onChatItemSwiped(abstractContact: AbstractContact)
}