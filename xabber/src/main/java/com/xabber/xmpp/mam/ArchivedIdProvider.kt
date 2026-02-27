package com.xabber.xmpp.mam

import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.xmlpull.v1.XmlPullParser

class ArchivedIdProvider: ExtensionElementProvider<ArchivedIdElement>() {

    override fun parse(parser: XmlPullParser?, initialDepth: Int): ArchivedIdElement {
        val id = parser?.getAttributeValue(null, ArchivedIdElement.ELEMENT)
        val by = parser?.getAttributeValue(null, ArchivedIdElement.ATTRIBUTE_BY)
        return ArchivedIdElement(id ?: "", by ?: "")
    }

}