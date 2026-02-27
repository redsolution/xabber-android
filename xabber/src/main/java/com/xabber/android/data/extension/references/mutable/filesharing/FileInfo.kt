package com.xabber.android.data.extension.references.mutable.filesharing

import org.jivesoftware.smack.util.XmlStringBuilder

/**
 * Represents a <file> element at file-sharing reference according to [XEP-FILE]
 * The <file/> child element is used to describe the properties of the file, which are represented in the <media/> element.
 *
 * @link https://xabber.com/protocol/otb
 *
 * @param mediaType The IANA-registered content type of the file data. Must be not empty.
 * @param name The name of file. Must be not empty.
 * @param size The size of the image data in bytes. Must be not equal to zero.
 */
class FileInfo(val mediaType: String, val name: String, val size: Long) {

    var desc: String = ""
    var height = 0
    var width = 0
    var duration: Long = 0

    fun toXML() = XmlStringBuilder().apply {
        openElement(FILE_ELEMENT)

        optElement(ELEMENT_MEDIA_TYPE, mediaType)
        optElement(ELEMENT_NAME, name)
        optElement(ELEMENT_SIZE, size.toString())

        if (height > 0) {
            optElement(ELEMENT_HEIGHT, height.toString())
        }
        if (width > 0) {
            optElement(ELEMENT_WIDTH, width)
        }
        if (desc.isNotEmpty()) {
            optElement(ELEMENT_DESC, desc)
        }
        if (duration > 0) {
            optElement(ELEMENT_DURATION, duration)
        }

        closeElement(FILE_ELEMENT)
    }

    companion object {
        const val FILE_ELEMENT = "file"
        const val ELEMENT_MEDIA_TYPE = "media-type"
        const val ELEMENT_NAME = "name"
        const val ELEMENT_DESC = "desc"
        const val ELEMENT_HEIGHT = "height"
        const val ELEMENT_WIDTH = "width"
        const val ELEMENT_SIZE = "size"
        const val ELEMENT_DURATION = "duration"
    }

}