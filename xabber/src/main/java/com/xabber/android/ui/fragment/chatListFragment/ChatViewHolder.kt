package com.xabber.android.ui.fragment.chatListFragment

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import github.ankushsachdeva.emojicon.EmojiconTextView

class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    var messageRealmObject: MessageRealmObject? = null
    var rosterStatus: Int? = null
    var accountColorIndicator: Int? = null

    val avatarIV: ImageView = itemView.findViewById(R.id.ivAvatar)
    val statusIV: ImageView = itemView.findViewById(R.id.ivStatus)
    val contactNameTV: TextView = itemView.findViewById(R.id.tvContactName)
    val messageTextTV: EmojiconTextView = itemView.findViewById(R.id.tvMessageText)
    val timeTV: TextView = itemView.findViewById(R.id.tvTime)
    val messageStatusTV: ImageView = itemView.findViewById(R.id.ivMessageStatus)
    val unreadCountTV: TextView = itemView.findViewById(R.id.tvUnreadCount)
    val accountColorIndicatorBackView: View = itemView.findViewById(R.id.accountColorIndicatorBack)
    val accountColorIndicatorView: View = itemView.findViewById(R.id.accountColorIndicator)

}