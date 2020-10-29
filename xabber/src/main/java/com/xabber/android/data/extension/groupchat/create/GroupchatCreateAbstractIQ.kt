package com.xabber.android.data.extension.groupchat.create

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ

abstract class GroupchatCreateAbstractIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK) {
    companion object{
        const val HASH_BLOCK = "#create"
    }
}