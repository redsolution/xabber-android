package com.xabber.android.data.extension.references.mutable.filesharing

import org.jivesoftware.smack.util.XmlStringBuilder

/**
 * Represents a <sources> element at file-sharing reference according to [XEP-FILE]
 * The <sources/> element is used to define available sources where clients can download the file.
 * This element must contain at least one child element <uri/>.
 * The <uri/> element has information about uri.
 *
 * @link https://xabber.com/protocol/otb
 */
class FileSources(val uris: List<String>) {

    constructor(uri: String): this(listOf<String>(uri))

    fun toXML() = XmlStringBuilder().apply {
        openElement(SOURCES_ELEMENT)

        for (uri in uris){
            optElement(URI_ELEMENT, uri)
        }

        closeElement(SOURCES_ELEMENT)
    }

    companion object {
        const val SOURCES_ELEMENT = "sources"
        const val URI_ELEMENT = "uri"
    }

}