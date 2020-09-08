package com.xabber.android.ui.adapter

import android.app.Activity
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.account.StatusMode
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.groupchat.GroupChat
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager

class SearchContactsListItemAdapter(val items: MutableList<AbstractChat>,
                                    val listener: SearchContactsListItemListener) :
        RecyclerView.Adapter<SearchContactsListItemAdapter.SearchContactsListItemViewHolder>(),
        View.OnClickListener {

    lateinit var recyclerView: RecyclerView
    lateinit var activity: Activity

    override fun getItemCount() = items.size

    override fun onClick(v: View?) {
        if (v != null && v.id == R.id.ivAvatar) {
            val position = recyclerView.getChildLayoutPosition(v.parent as View)
            listener.onContactListItemClick(items[position])
        } else {
            val position = recyclerView.getChildLayoutPosition(v!!)
            listener.onContactListItemClick(items[position])
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            SearchContactsListItemViewHolder(LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.item_discover_contact_list_item, parent, false))


    override fun onBindViewHolder(holder: SearchContactsListItemViewHolder, position: Int) {
        holder.itemView.setOnClickListener(this)

        /* Setup avatar */
        if (SettingsManager.contactsShowAvatars()) {
            holder.avatarIv.visibility = View.VISIBLE
            holder.avatarIv.setImageDrawable(RosterManager.getInstance()
                    .getAbstractContact(items[position].account, items[position].contactJid)
                    .getAvatar(true))
        } else {
            holder.avatarIv.visibility = View.GONE
        }

        /* Setup roster status */
        var statusLevel = RosterManager.getInstance()
                .getAbstractContact(items[position].account, items[position].contactJid)
                .statusMode
                .statusLevel

        val isServer = items[position].contactJid.jid.isDomainBareJid
        val isBlocked = BlockingManager.getInstance()
                .contactIsBlockedLocally(items[position].account, items[position].contactJid)
        val isVisible = holder.avatarIv.visibility == View.VISIBLE
        val isUnavailable = statusLevel == StatusMode.unavailable.ordinal
        val isAccountConnected = AccountManager.getInstance().connectedAccounts
                .contains(items[position].account)
        val isGroupchat = items[position] is GroupChat
        val rosterContact = RosterManager.getInstance()
                .getRosterContact(items[position].account, items[position].contactJid)

        when {
            isBlocked -> statusLevel = 11
            isServer -> statusLevel = 10
            isGroupchat -> statusLevel = 9
        }

        holder.statusIv.setImageLevel(statusLevel)

        holder.statusIv.visibility =
                if (!isServer && !isGroupchat && !isBlocked && isVisible
                        && (isUnavailable || !isAccountConnected))
                    View.INVISIBLE
                else
                    View.VISIBLE

        if ((isServer || isGroupchat) && !isAccountConnected) {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val colorFilter = ColorMatrixColorFilter(colorMatrix)
            holder.statusIv.colorFilter = colorFilter
        } else
            holder.statusIv.setColorFilter(0)

        /* Setup name */
        val name = RosterManager.getInstance().getBestContact(items[position].account,
                items[position].contactJid).name.split(" ")[0]
        if (AccountManager.getInstance().enabledAccounts.size > 1){
            val spannableString = SpannableString(name)
            val color = ForegroundColorSpan(ColorManager.getInstance().accountPainter
                    .getAccountMainColor(items[position].account))

            spannableString.setSpan(color, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.contactNameTv.setText(spannableString, TextView.BufferType.SPANNABLE)
        } else holder.contactNameTv.text = name

    }


    interface SearchContactsListItemListener : View.OnCreateContextMenuListener {
        fun onContactListItemClick(contact: AbstractChat)
    }

    class SearchContactsListItemViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
        var chat: AbstractChat? = null
        val avatarIv = itemView.findViewById<ImageView>(R.id.ivAvatar)
        val statusIv = itemView.findViewById<ImageView>(R.id.ivStatus)
        val contactNameTv = itemView
                .findViewById<TextView>(R.id.search_contact_list_item_name_text_view)
    }
}

