package com.xabber.android.ui.adapter.chat

import android.content.Context
import android.view.View
import com.xabber.android.data.database.realmobjects.MessageRealmObject
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.helper.dipToPx
import com.xabber.android.ui.text.getDateStringForMessage

class ActionMessageVH(itemView: View?) : BasicMessageVH(itemView) {

    fun bind(
        messageRealmObject: MessageRealmObject,
        context: Context?,
        account: AccountJid?,
        needDate: Boolean
    ) {
        messageText.text = messageRealmObject.chatAction.getText(
            context,
            RosterManager.getInstance().getBestContact(account, messageRealmObject.user).name,
            messageRealmObject.spannable.toString()
        )

        this.needDate = needDate
        date = getDateStringForMessage(messageRealmObject.timestamp)

        itemView.setPadding(
            itemView.paddingLeft,
            dipToPx(4f, itemView.context),
            itemView.paddingRight,
            dipToPx(4f, itemView.context)
        )
    }

}