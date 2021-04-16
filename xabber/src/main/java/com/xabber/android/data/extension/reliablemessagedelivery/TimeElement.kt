package com.xabber.android.data.extension.reliablemessagedelivery

import com.xabber.android.data.extension.reliablemessagedelivery.TimeElement.Companion.ELEMENT
import com.xabber.android.data.extension.reliablemessagedelivery.TimeElement.Companion.NAMESPACE
import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.util.XmlStringBuilder

class TimeElement(val by: String,
                  val timeStamp: String,
): ExtensionElement {

    override fun getNamespace() = NAMESPACE

    override fun getElementName() = ELEMENT

    override fun toXML() = XmlStringBuilder(this).apply {
        attribute(ATTRIBUTE_BY, by)
        attribute(ATTRIBUTE_STAMP, timeStamp)
        closeEmptyElement()
    }

    companion object {
        const val NAMESPACE = DeliveryManager.NAMESPACE
        const val ELEMENT = "time"
        const val ATTRIBUTE_BY = "by"
        const val ATTRIBUTE_STAMP = "stamp"
    }

}

fun Stanza.hasTimeElement() = this.hasExtension(ELEMENT, NAMESPACE)
fun Stanza.getTimeElement() = this.getExtension<TimeElement>(ELEMENT, NAMESPACE)