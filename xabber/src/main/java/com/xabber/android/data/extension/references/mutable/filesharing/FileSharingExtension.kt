package com.xabber.android.data.extension.references.mutable.filesharing

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.util.XmlStringBuilder

/**
 * Represents the <file-sharing/> element is used to represent information about a file according to [XEP-FILE]
 * This element has two allowable children: <file/> <sources/>
 *
 * @link https://xabber.com/protocol/otb
 */
class FileSharingExtension(var fileInfo: FileInfo, var fileSources: FileSources) : ExtensionElement {

    override fun getNamespace() = NAMESPACE

    override fun getElementName() = FILE_SHARING_ELEMENT

    override fun toXML() = XmlStringBuilder().apply {
        prelude(FILE_SHARING_ELEMENT, NAMESPACE)
        rightAngleBracket()
        append(fileInfo.toXML())
        append(fileSources.toXML())
        closeElement(FILE_SHARING_ELEMENT)
    }

    companion object {
        const val NAMESPACE = "https://xabber.com/protocol/files"
        const val FILE_SHARING_ELEMENT = "file-sharing"
    }

}