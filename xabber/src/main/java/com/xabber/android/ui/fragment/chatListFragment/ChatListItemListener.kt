package com.xabber.android.ui.fragment.chatListFragment

import android.view.ContextMenu
import android.view.View
import com.xabber.android.data.database.realmobjects.ChatRealmObject
import com.xabber.android.data.roster.AbstractContact

interface ChatListItemListener: View.OnCreateContextMenuListener{
    fun onChatItemClick(contact: ChatRealmObject)
    fun onChatAvatarClick(contact: ChatRealmObject)
    fun onChatItemContextMenu(menu: ContextMenu, contact: ChatRealmObject)
    fun onChatItemSwiped(abstractContact: ChatRealmObject)
    fun onListBecomeEmpty()
}