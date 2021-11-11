package com.xabber.android.data.extension.references.mutable.filesharing

import com.xabber.android.data.extension.references.mutable.Mutable
import org.jivesoftware.smack.util.XmlStringBuilder

class FileReference(begin: Int, end: Int, val fileSharingExtensions: List<FileSharingExtension>) : Mutable(begin, end) {

    constructor(begin: Int, end: Int, fileSharingExtension: FileSharingExtension)
            : this(begin, end, listOf(fileSharingExtension))

    override fun appendToXML(xml: XmlStringBuilder) {
        if (fileSharingExtensions.isNotEmpty()) {
            for (file in fileSharingExtensions) {
                xml.append(file.toXML())
            }
        }
    }

}