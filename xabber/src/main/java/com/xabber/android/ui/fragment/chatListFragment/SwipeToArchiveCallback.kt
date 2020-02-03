package com.xabber.android.ui.fragment.chatListFragment

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R

class SwipeToArchiveCallback(private val adapter: ChatListAdapter) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT){

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder):
            Boolean = false

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val context = recyclerView.context
        val icon = context.resources.getDrawable(R.drawable.ic_arcived)
        val itemView = viewHolder.itemView
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.action_with_chat_background, typedValue, true)
        val background = ColorDrawable(typedValue.data)

        val backgroundOffset = 20
        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight

        if (dX > 0){
            val iconLeft = itemView.left + iconMargin
            val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + backgroundOffset, itemView.bottom  )
        } else if (dX < 0){
            val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
            val iconRight = itemView.right - iconMargin
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            background.setBounds(itemView.right + dX.toInt() - backgroundOffset, itemView.top, itemView.right, itemView.bottom)
        } else background.setBounds(0,0,0,0)

        background.draw(c)
        icon.draw(c)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) =
            adapter.onSwipeChatItem(viewHolder as ChatViewHolder)

}