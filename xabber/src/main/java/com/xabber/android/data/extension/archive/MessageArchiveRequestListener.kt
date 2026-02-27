package com.xabber.android.data.extension.archive

import com.xabber.android.data.database.realmobjects.MessageRealmObject

interface MessageArchiveRequestListener {
    fun onMessagesReceived(messagesList: List<MessageRealmObject>)
    fun onErrorReceived(exception: Exception? = null)
}