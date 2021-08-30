package com.xabber.xmpp

import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder

class SimpleNamedElement(val name: String, val value: String? = null) : NamedElement {

    override fun getElementName() = name

    override fun toXML() = XmlStringBuilder(this).apply {
        rightAngleBracket()
        value?.let { append(value) }
        closeElement(name)
    }

}