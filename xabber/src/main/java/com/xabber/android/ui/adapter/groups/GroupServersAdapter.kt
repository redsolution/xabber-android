package com.xabber.android.ui.adapter.groups

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.xabber.android.R
import com.xabber.android.data.extension.groups.GroupServerType

class GroupServersAdapter(
    private val serversMap: MutableList<Pair<String, GroupServerType>>, private val listener: OnClickListener
) : BaseAdapter() {

    override fun getCount() = serversMap.size

    override fun getItem(position: Int) = serversMap[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        when (serversMap[position].second) {

            GroupServerType.none -> {
                LayoutInflater.from(parent.context).inflate(R.layout.group_server_list_item_none, parent, false)
                    .also { it.setOnClickListener { listener.onCustomClicked() } }
            }

            GroupServerType.providedByXabber -> {
                LayoutInflater.from(parent.context).inflate(R.layout.group_server_list_item_available, parent, false)
                    .also {
                        it.findViewById<TextView>(R.id.text_view).apply {
                            text = getItem(position).first
                            setOnClickListener { listener.onServerClicked(getItem(position).first) }
                        }
                        it.findViewById<ImageView>(R.id.image_view).setOnClickListener {
                            Toast.makeText(
                                parent.context,
                                parent.context.getString(R.string.groupchat_provided_by_xabber),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }

            GroupServerType.custom -> {
                LayoutInflater.from(parent.context).inflate(R.layout.group_server_list_item_custom, parent, false)
                    .also {
                        it.findViewById<TextView>(R.id.text_view).apply {
                            text = getItem(position).first
                            setOnClickListener { listener.onServerClicked(getItem(position).first) }
                        }
                        it.findViewById<ImageView>(R.id.image_view).setOnClickListener {
                            listener.onServerDeleted(getItem(position).first)
                            for (server in serversMap) {
                                if (server.first == getItem(position).first) {
                                    serversMap.removeAt(position)
                                    break
                                }
                            }
                            notifyDataSetChanged()
                        }
                    }
            }

            GroupServerType.providedByOwnServer -> {
                LayoutInflater.from(parent.context).inflate(R.layout.group_server_list_item_available, parent, false)
                    .also {
                        it.findViewById<TextView>(R.id.text_view).apply {
                            text = getItem(position).first
                            setOnClickListener { listener.onServerClicked(getItem(position).first) }
                        }
                        it.findViewById<ImageView>(R.id.image_view).setOnClickListener {
                            Toast.makeText(
                                parent.context, parent.context.getString(R.string.groupchat_provided_by_your_server),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }

        }

    interface OnClickListener {
        fun onServerClicked(server: String)
        fun onServerDeleted(server: String)
        fun onCustomClicked()
    }

}