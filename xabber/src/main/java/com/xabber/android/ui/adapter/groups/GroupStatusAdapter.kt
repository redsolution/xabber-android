package com.xabber.android.ui.adapter.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.account.StatusMode
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.message.chat.groupchat.GroupchatPrivacyType
import com.xabber.android.data.roster.StatusBadgeSetupHelper
import org.jivesoftware.smackx.xdata.FormField

class GroupStatusAdapter(private val statuses: List<FormField.Option>, val groupchat: GroupChat,
                         private val descriptions: List<FormField>, val listener: Listener)
    : RecyclerView.Adapter<GroupStatusVH>(){

    override fun getItemCount() = statuses.size

    override fun onBindViewHolder(holder: GroupStatusVH, position: Int) {
        val option = statuses[position]
        holder.bind(option, descriptions[position], object: GroupStatusVH.Listener{
            override fun onClick() {
                listener.onStatusClicked(option)
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = GroupStatusVH(
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.group_status_item_vh, parent, false),
            groupchat)

    interface Listener{
        fun onStatusClicked(option: FormField.Option)
    }

}

class GroupStatusVH(itemView: View, val groupchat: GroupChat): RecyclerView.ViewHolder(itemView){

    fun bind(status: FormField.Option, description: FormField, listener: Listener){
        itemView.findViewById<TextView>(R.id.group_status_item_tv).text = status.label

        val statusLevelOffset = if (groupchat.privacyType == GroupchatPrivacyType.INCOGNITO)
            StatusMode.INCOGNITO_GROUP_OFFSET
        else StatusMode.PUBLIC_GROUP_OFFSET

        StatusBadgeSetupHelper.setupImageView(StatusMode.createStatusMode(description.values[0]),
                statusLevelOffset, itemView.findViewById(R.id.group_status_item_iv))

        itemView.setOnClickListener { listener.onClick() }
        itemView.setOnLongClickListener {
            Toast.makeText(itemView.context, description.description, Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }
    }

    interface Listener{
        fun onClick()
    }

}