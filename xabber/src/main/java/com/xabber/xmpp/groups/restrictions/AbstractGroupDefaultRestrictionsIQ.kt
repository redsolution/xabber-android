package com.xabber.xmpp.groups.restrictions

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ

abstract class AbstractGroupDefaultRestrictionsIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK) {
    companion object {
        const val HASH_BLOCK = "#default-rights"
    }

}