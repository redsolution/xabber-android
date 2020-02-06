package com.xabber.android.ui.fragment.chatListFragment

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.database.messagerealm.MessageItem
import github.ankushsachdeva.emojicon.EmojiconTextView

class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    var messageItem: MessageItem? = null
    var rosterStatus: Int? = null
    var accountColorIndicator: Int? = null

    val avatarIV = itemView.findViewById<ImageView>(R.id.ivAvatar)
    val statusGroupchatIV = itemView.findViewById<ImageView>(R.id.ivStatusGroupchat)
    val statusIV = itemView.findViewById<ImageView>(R.id.ivStatus)
    val onlyStatusIV = itemView.findViewById<ImageView>(R.id.ivOnlyStatus)
    val actionTV = itemView.findViewById<TextView>(R.id.tvAction)
    val actionLeftTV = itemView.findViewById<TextView>(R.id.tvActionLeft)
    val contactNameTV = itemView.findViewById<TextView>(R.id.tvContactName)
    val messageTextTV = itemView.findViewById<EmojiconTextView>(R.id.tvMessageText)
    val timeTV = itemView.findViewById<TextView>(R.id.tvTime)
    val messageStatusTV = itemView.findViewById<ImageView>(R.id.ivMessageStatus)
    val unreadCountTV = itemView.findViewById<TextView>(R.id.tvUnreadCount)
    val accountColorIndicatorBackView = itemView.findViewById<View>(R.id.accountColorIndicatorBack)
    val accountColorIndicatorView = itemView.findViewById<View>(R.id.accountColorIndicator)

}