package com.xabber.android.ui.fragment.chatListFragment

import android.app.Activity
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.database.realmobjects.ChatRealmObject
import com.xabber.android.data.roster.AbstractContact

class ChatListAdapter(val list: MutableList<ChatRealmObject>, val listener: ChatListItemListener) :
        RecyclerView.Adapter<ChatViewHolder>(), View.OnClickListener, View.OnCreateContextMenuListener{

    lateinit var recyclerView: RecyclerView
    lateinit var activity: Activity
    val holdersMap: HashMap<Int, ChatViewHolder> = HashMap()

    override fun getItemCount(): Int = list.size

    fun getAbstractContactFromPosition(position: Int) = list[position]

    fun getAbstractContactFromView(v: View?) =
            getAbstractContactFromPosition(recyclerView.getChildLayoutPosition(v!!))

    fun addItem(index: Int, item: ChatRealmObject) {
        this.list.add(index, item)
        notifyItemChanged(index)
    }

    fun addItems(newItemsList: MutableList<ChatRealmObject>){
        this.list.clear()
        this.list.addAll(newItemsList)
    }

    fun deleteItemByPosition(position: Int) {
        list.removeAt(position)
        holdersMap.remove(position)
        notifyDataSetChanged()
    }

    fun deleteItemByAbstractContact(contact: ChatRealmObject) =
            deleteItemByPosition(list.indexOf(contact))

    fun onSwipeChatItem(holder: ChatViewHolder){
        val swipedContact = getAbstractContactFromView(holder.itemView)
        deleteItemByAbstractContact(swipedContact)
        listener.onChatItemSwiped(swipedContact)
        if (itemCount == 0) listener.onListBecomeEmpty()
    }

    override fun onClick(v: View?) {
        if (v!!.id == R.id.ivAvatar) listener.onChatAvatarClick(
                list[recyclerView.getChildLayoutPosition(v.parent as View)])
        else listener.onChatItemClick(list[recyclerView.getChildLayoutPosition(v)])
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) =
        listener.onChatItemContextMenu(menu!!, getAbstractContactFromView(v))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
            ChatViewHolder(LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_chat_in_contact_list_new, parent, false))

    override fun onAttachedToRecyclerView(recycler: RecyclerView) {
        recyclerView = recycler
        ItemTouchHelper(SwipeToArchiveCallback(this)).attachToRecyclerView(recycler)
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.itemView.setOnClickListener(this)
        holder.avatarIV.setOnClickListener(this)
        holder.itemView.setOnCreateContextMenuListener(this)

        SetupChatItemViewHolderHelper(holder, list[position]).setup()
        holdersMap.put(position, holder)
    }

}