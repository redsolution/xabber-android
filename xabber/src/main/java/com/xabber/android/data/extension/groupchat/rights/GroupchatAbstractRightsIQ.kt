package com.xabber.android.data.extension.groupchat.rights

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ

abstract class GroupchatAbstractRightsIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK) {

    companion object {
        const val HASH_BLOCK = "#rights"
    }

}