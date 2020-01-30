package com.xabber.android.ui.fragment.chatListFragment

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToArchiveCallback(private val adapter: ChatListAdapter) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT){

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder):
            Boolean = false



    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) =
            adapter.onSwipeChatItem(viewHolder as ChatViewHolder)

}