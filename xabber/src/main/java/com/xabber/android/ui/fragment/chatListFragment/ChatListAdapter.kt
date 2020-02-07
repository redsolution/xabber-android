package com.xabber.android.ui.fragment.chatListFragment

import android.app.Activity
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R

class ChatListAdapter(val list: MutableList<ChatItemVO>, val listener: ChatListItemListener) :
        RecyclerView.Adapter<ChatViewHolder>(), View.OnClickListener, View.OnCreateContextMenuListener{

    lateinit var recyclerView: RecyclerView
    lateinit var activity: Activity

    override fun getItemCount(): Int = list.size

    fun getAbstractContactFromPosition(position: Int) = list[position]

    fun getAbstractContactFromView(v: View?) =
            getAbstractContactFromPosition(recyclerView.getChildLayoutPosition(v!!))

    fun addItem(index: Int, item: ChatItemVO) {
        this.list.add(index, item)
        notifyItemChanged(index)
    }

    fun addItems(newItemsList: MutableList<ChatItemVO>){
        this.list.clear()
        this.list.addAll(newItemsList)
    }

    fun deleteItemByPosition(position: Int) {
        list.removeAt(position)
        notifyDataSetChanged()
    }

    fun deleteItemByAbstractContact(contact: ChatItemVO) =
            deleteItemByPosition(list.indexOf(contact))

    fun onSwipeChatItem(holder: ChatViewHolder){
        val swipedContact = getAbstractContactFromView(holder.itemView)
        deleteItemByAbstractContact(swipedContact)
        listener.onChatItemSwiped(swipedContact)
        if (itemCount == 0) listener.onListBecomeEmpty()
    }

    override fun onClick(v: View?) {
        if (v!!.id == R.id.ivAvatar) listener.onChatAvatarClick(
                list[recyclerView.getChildLayoutPosition(v.parent.parent.parent.parent as View)])
        else listener.onChatItemClick(list[recyclerView.getChildLayoutPosition(v)])
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) =
        listener.onChatItemContextMenu(menu!!, getAbstractContactFromView(v))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
            ChatViewHolder(LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_chat_in_contact_list, parent, false))

    override fun onAttachedToRecyclerView(recycler: RecyclerView) {
        recyclerView = recycler
        ItemTouchHelper(SwipeToArchiveCallback(this)).attachToRecyclerView(recycler)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.itemView.setOnClickListener(this)
        holder.avatarIV.setOnClickListener(this)
        holder.itemView.setOnCreateContextMenuListener(this)

        val chatItemVO = list[position]

        holder.avatarIV.setImageDrawable(chatItemVO.contactAvatarDrawable)
        holder.avatarIV.visibility = chatItemVO.contactAvatarVisibility

        holder.statusGroupchatIV.setImageDrawable(chatItemVO.groupchatIndicatorDrawable)
        holder.statusGroupchatIV.visibility = chatItemVO.groupchatIndicatorVisibility

        holder.messageStatusTV.setImageDrawable(chatItemVO.messageStatusDrawable)
        holder.messageStatusTV.visibility = chatItemVO.messageStatusDrawableVisibility

        holder.accountColorIndicatorView.setBackgroundColor(chatItemVO.accountColorIndicator)
        holder.accountColorIndicatorView.visibility = chatItemVO.accountColorIndicatorVisibility

        holder.accountColorIndicatorBackView.setBackgroundColor(chatItemVO.accountColorIndicator)
        holder.accountColorIndicatorBackView.visibility = chatItemVO.accountColorIndicatorVisibility

        holder.onlyStatusIV.visibility = chatItemVO.contactOnlyStatusVisibility

        holder.statusIV.setImageLevel(chatItemVO.contactStatusLevel)
        holder.statusIV.visibility = chatItemVO.contactStatusVisibility

        holder.contactNameTV.text = chatItemVO.contactName

        if (chatItemVO.notificationMuteIcon != null)
            holder.contactNameTV.setCompoundDrawablesWithIntrinsicBounds(null, null,
                    if (chatItemVO.notificationMuteIcon != 0)
                        holder.itemView.resources.getDrawable(chatItemVO.notificationMuteIcon) else null,
                    null)

        holder.messageTextTV.text = chatItemVO.messageSpannedText?: chatItemVO.messageText
        if (chatItemVO.messageTextColor != null)
            holder.messageTextTV.setTextColor(chatItemVO.messageTextColor)

        holder.timeTV.text = chatItemVO.lastMessageTime

        holder.unreadCountTV.text = chatItemVO.unreadCount.toString()
        holder.unreadCountTV.visibility = chatItemVO.unreadCountVisibility


    }

}