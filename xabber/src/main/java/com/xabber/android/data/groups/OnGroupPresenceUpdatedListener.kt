package com.xabber.android.data.groups

import com.xabber.android.data.BaseUIListener
import com.xabber.android.data.entity.ContactJid

interface OnGroupPresenceUpdatedListener: BaseUIListener {
    fun onGroupPresenceUpdated(groupJid: ContactJid)
}