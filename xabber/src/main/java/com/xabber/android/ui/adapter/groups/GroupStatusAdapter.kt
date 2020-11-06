package com.xabber.android.ui.adapter.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.account.StatusMode
import org.jivesoftware.smackx.xdata.FormField

class GroupStatusAdapter(private val statuses: List<FormField.Option>,
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
            LayoutInflater.from(parent.context).inflate(R.layout.group_status_item_vh, parent, false))

    interface Listener{
        fun onStatusClicked(option: FormField.Option)
    }

}

class GroupStatusVH(itemView: View): RecyclerView.ViewHolder(itemView){

    fun bind(status: FormField.Option, description: FormField, listener: Listener){
        itemView.findViewById<TextView>(R.id.group_status_item_tv).text = status.label
        itemView.findViewById<ImageView>(R.id.group_status_item_iv).setImageLevel(
                StatusMode.createStatusMode(description.values[0]).statusLevel)
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