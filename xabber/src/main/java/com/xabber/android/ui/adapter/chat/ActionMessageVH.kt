package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.utils.StringUtils
import com.xabber.android.utils.Utils

class ActionMessageVH(itemView: View?) : BasicMessageVH(itemView) {

    fun bind(messageRealmObject: MessageRealmObject, context: Context?, account: AccountJid?, needDate: Boolean) {
        val name = RosterManager.getInstance().getBestContact(account, messageRealmObject.user).name

        messageText.text = messageRealmObject.chatAction.getText(context, name, messageRealmObject.spannable.toString())

        this.needDate = needDate
        date = StringUtils.getDateStringForMessage(messageRealmObject.timestamp)

        itemView.setPadding(
            itemView.paddingLeft,
            Utils.dipToPx(4f, itemView.context),
            itemView.paddingRight,
            Utils.dipToPx(4f, itemView.context)
        )
    }

}