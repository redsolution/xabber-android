package com.xabber.xmpp.groups

import com.xabber.android.data.extension.groups.GroupsManager
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.XmlStringBuilder

open class GroupExtensionElement : ExtensionElement {

    override fun getNamespace() = NAMESPACE

    override fun getElementName() = ELEMENT

    override fun toXML() = XmlStringBuilder(this).apply {
        rightAngleBracket()
        appendToXML(this)
        closeElement(elementName)
    }

    open fun appendToXML(xml: XmlStringBuilder?) {}

    companion object {
        const val NAMESPACE = GroupsManager.NAMESPACE
        const val ELEMENT = "x"
    }

}

fun Stanza.hasGroupExtensionElement() =
    this.hasExtension(GroupExtensionElement.ELEMENT, GroupExtensionElement.NAMESPACE)

fun Stanza.getGroupExtensionElement(): GroupExtensionElement? =
    this.getExtension(GroupExtensionElement.ELEMENT, GroupExtensionElement.NAMESPACE)

fun Stanza.hasGroupSystemMessage() =
    this.hasExtension(GroupExtensionElement.ELEMENT, GroupsManager.SYSTEM_MESSAGE_NAMESPACE)
