package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.utils.StringUtils

class ActionMessageVH(itemView: View?) : BasicMessageVH(itemView) {

    fun bind(messageRealmObject: MessageRealmObject, context: Context?, account: AccountJid?, needDate: Boolean) {
        val name = RosterManager.getInstance().getBestContact(account, messageRealmObject.user).name

        messageText.text = MessageRealmObject.getChatAction(messageRealmObject)
                .getText(context, name, MessageRealmObject.getSpannable(messageRealmObject).toString())

        this.needDate = needDate
        date = StringUtils.getDateStringForMessage(messageRealmObject.timestamp)
    }

}