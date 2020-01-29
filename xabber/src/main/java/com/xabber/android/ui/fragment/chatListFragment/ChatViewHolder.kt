package com.xabber.android.ui.fragment.chatListFragment

import android.view.ContextMenu
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import github.ankushsachdeva.emojicon.EmojiconTextView

class ChatViewHolder(itemView: View, val listener: ChatItemClickListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnCreateContextMenuListener {
    val avatarIV = itemView.findViewById<ImageView>(R.id.ivAvatar)
    val statusGroupchatIV = itemView.findViewById<ImageView>(R.id.ivStatusGroupchat)
    val statusIV = itemView.findViewById<ImageView>(R.id.ivStatus)
    val onlyStatusIV = itemView.findViewById<ImageView>(R.id.ivOnlyStatus)
    val actionTV = itemView.findViewById<TextView>(R.id.tvAction)
    val actionLeftTV = itemView.findViewById<TextView>(R.id.tvActionLeft)
    val foregroundLayout = itemView.findViewById<LinearLayout>(R.id.foregroundView)
    val contactNameTV = itemView.findViewById<TextView>(R.id.tvContactName)
    val outgoingMessageTV = itemView.findViewById<TextView>(R.id.tvOutgoingMessage)
    val messageTextTV = itemView.findViewById<EmojiconTextView>(R.id.tvMessageText)
    val timeTV = itemView.findViewById<TextView>(R.id.tvTime)
    val unreadCountView = itemView.findViewById<RelativeLayout>(R.id.rlUnread)
    val messageStatusTV = itemView.findViewById<ImageView>(R.id.ivMessageStatus)
    val unreadCountTV = itemView.findViewById<TextView>(R.id.tvUnreadCount)
    val accountColorIndicatorBackView = itemView.findViewById<View>(R.id.accountColorIndicatorBack)
    val accountColorIndicatorView = itemView.findViewById<View>(R.id.accountColorIndicator)

    override fun onClick(v: View?) {
        if (layoutPosition != RecyclerView.NO_POSITION) {
            if (v?.id == R.id.ivAvatar) listener.onAvatarClick(layoutPosition)
            else listener.onChatClick(layoutPosition)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        if (layoutPosition != RecyclerView.NO_POSITION) listener.onContextMenuCreated(layoutPosition, menu)
    }

    interface ChatItemClickListener {
        fun onChatClick(position: Int)
        fun onAvatarClick(position: Int)
        fun onContextMenuCreated(position: Int, menu: ContextMenu?)
        fun onContextMenuItemClick()//TODO args
    } //TODO contextMenu

}