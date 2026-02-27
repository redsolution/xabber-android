package com.xabber.xmpp.groups.create

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ

abstract class GroupchatCreateAbstractIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK) {
    companion object{
        const val HASH_BLOCK = "#create"
    }
}