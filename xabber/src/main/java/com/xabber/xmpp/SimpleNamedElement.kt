package com.xabber.xmpp

import org.jivesoftware.smack.packet.NamedElement
import org.jivesoftware.smack.util.XmlStringBuilder

class SimpleNamedElement(val name: String, val value: String): NamedElement {

    override fun getElementName() = name

    override fun toXML() = XmlStringBuilder(this).apply {
        rightAngleBracket()
        append(value)
        closeElement(name)
    }

}