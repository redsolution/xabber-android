package com.xabber.android.ui.fragment.chatListFragment

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.message.chat.AbstractChat

class ChatListAdapter(
    val list: MutableList<AbstractChat>,
    val listener: ChatListItemListener,
    private val swipable: Boolean = true
) : RecyclerView.Adapter<ChatViewHolder>(), View.OnClickListener, View.OnCreateContextMenuListener {

    lateinit var recyclerView: RecyclerView
    private val holdersMap: HashMap<Int, ChatViewHolder> = HashMap()

    var isSavedMessagesSpecialText: Boolean = false

    override fun getItemCount(): Int = list.size

    private fun getAbstractContactFromPosition(position: Int) = list[position]

    private fun getAbstractContactFromView(v: View) =
        getAbstractContactFromPosition(recyclerView.getChildLayoutPosition(v))

    fun addItem(index: Int, item: AbstractChat) {
        list.add(index, item)
        notifyItemChanged(index)
    }

    fun addItems(newItemsList: MutableList<AbstractChat>) {
        list.clear()
        list.addAll(newItemsList)
        notifyDataSetChanged()
    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }

    private fun deleteItemByPosition(position: Int) {
        list.removeAt(position)
        holdersMap.remove(position)
        notifyDataSetChanged()
    }

    private fun deleteItemByAbstractContact(contact: AbstractChat) =
        deleteItemByPosition(list.indexOf(contact))

    fun onSwipeChatItem(holder: ChatViewHolder) {
        val swipedContact = getAbstractContactFromView(holder.itemView)
        deleteItemByAbstractContact(swipedContact)
        listener.onChatItemSwiped(swipedContact)
        if (itemCount == 0) listener.onListBecomeEmpty()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.ivAvatar) {
            listener.onChatAvatarClick(list[recyclerView.getChildLayoutPosition(v.parent as View)])
        } else listener.onChatItemClick(list[recyclerView.getChildLayoutPosition(v)])
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) =
        listener.onChatItemContextMenu(menu, getAbstractContactFromView(v))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
        ChatViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_in_contact_list, parent, false),
            this
        )

    override fun onAttachedToRecyclerView(recycler: RecyclerView) {
        recyclerView = recycler
        if (swipable) ItemTouchHelper(SwipeToArchiveCallback(this)).attachToRecyclerView(recycler)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.itemView.setOnClickListener(this)
        holder.itemView.setOnCreateContextMenuListener(this)

        holder.bind(
            ChatListItemData.createFromChat(list[position], holder.itemView.context), isSavedMessagesSpecialText
        )

        holdersMap[position] = holder
    }

}