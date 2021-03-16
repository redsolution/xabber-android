package com.xabber.android.data.extension.groupchat.status

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ

abstract class AbstractGroupStatusIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK){
    companion object{
        const val HASH_BLOCK = "#status"
    }
}