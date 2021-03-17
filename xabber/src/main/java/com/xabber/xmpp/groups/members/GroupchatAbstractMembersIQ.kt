package com.xabber.xmpp.groups.members

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ

abstract class GroupchatAbstractMembersIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK) {
    companion object {
        const val HASH_BLOCK = "#members"
    }
}