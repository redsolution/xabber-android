package com.xabber.xmpp.groups.system_message

import com.xabber.xmpp.groups.GroupchatExtensionElement
import org.jivesoftware.smack.packet.ExtensionElement

class GroupSystemMessageExtensionElement: GroupchatExtensionElement() {

    var type: Type = Type.none
    val listOfExtensions = mutableListOf<ExtensionElement>()

    override fun getNamespace(): String {
        return super.getNamespace() + HASH_BLOCK
    }

    companion object{
        const val HASH_BLOCK = "#system-message"
        const val TYPE_ATTRIBUTE = "type"
    }

    enum class Type {
        join, kick, block, left, echo, create, none
    }

}