package com.xabber.android.data.extension.groupchat.restrictions

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ

abstract class AbstractGroupDefaultRestrictionsIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK) {
    companion object {
        const val HASH_BLOCK = "#default-rights"
    }

}