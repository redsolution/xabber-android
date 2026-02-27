package com.xabber.xmpp.groups.rights

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ

abstract class GroupchatAbstractRightsIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK) {

    companion object {
        const val HASH_BLOCK = "#rights"
    }

}