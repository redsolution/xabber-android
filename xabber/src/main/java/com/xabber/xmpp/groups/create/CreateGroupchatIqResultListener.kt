package com.xabber.xmpp.groups.create

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

interface CreateGroupchatIqResultListener {
    fun onSend()
    fun onJidConflict()
    fun onOtherError()
    fun onSuccessfullyCreated(accountJid: AccountJid?, contactJid: ContactJid?)
}