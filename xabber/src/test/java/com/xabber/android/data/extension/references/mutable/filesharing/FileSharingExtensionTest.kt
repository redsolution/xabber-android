package com.xabber.android.data.extension.references.mutable.filesharing

import junit.framework.TestCase

class FileSharingExtensionTest : TestCase() {

    private val sources = FileSources("first.source.domain/hello")

    private val fileInfo = FileInfo("media/type", "filename", 1024)

    private val fileSharingExtension = FileSharingExtension(fileInfo, sources)

    fun test_getNamespace() {
        assertEquals("https://xabber.com/protocol/files", fileSharingExtension.namespace)
    }

    fun testGetElementName() {
        assertEquals("file-sharing", fileSharingExtension.elementName)
    }

    fun testToXML() {
        val reference =
                    "<file-sharing xmlns='https://xabber.com/protocol/files'>" +
                        "<file>" +
                            "<media-type>media/type</media-type>" +
                            "<name>filename</name>" +
                            "<size>1024</size>" +
                        "</file>" +
                        "<sources>" +
                            "<uri>first.source.domain/hello</uri>" +
                        "</sources>" +
                    "</file-sharing>"
        assertEquals(reference, fileSharingExtension.toXML().toString())
    }

}