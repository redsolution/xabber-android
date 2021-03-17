package com.xabber.xmpp.groups.status

import com.xabber.xmpp.groups.GroupchatAbstractQueryIQ

abstract class AbstractGroupStatusIQ: GroupchatAbstractQueryIQ(NAMESPACE + HASH_BLOCK){
    companion object{
        const val HASH_BLOCK = "#status"
    }
}