package com.xabber.android.data.extension.groupchat.members

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ

abstract class GroupchatAbstractMembersIQ: GroupchatAbstractQueryIQ(ELEMENT, NAMESPACE + HASH_BLOCK) {

    companion object {
        const val HASH_BLOCK = "#members"
    }

}