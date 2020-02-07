package com.xabber.android.ui.fragment.chatListFragment

import android.view.ContextMenu
import android.view.View

interface ChatListItemListener: View.OnCreateContextMenuListener{
    fun onChatItemClick(chatItemVO: ChatItemVO)
    fun onChatAvatarClick(chatItemVO: ChatItemVO)
    fun onChatItemContextMenu(menu: ContextMenu, chatItemVO: ChatItemVO)
    fun onChatItemSwiped(chatItemVO: ChatItemVO)
    fun onListBecomeEmpty()
}