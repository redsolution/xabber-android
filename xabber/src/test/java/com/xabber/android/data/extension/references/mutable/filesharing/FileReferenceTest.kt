package com.xabber.android.data.extension.references.mutable.filesharing

import junit.framework.TestCase
import org.jivesoftware.smack.util.XmlStringBuilder

class FileReferenceTest : TestCase() {

    private fun getFileReferenceWithOneChild(): FileReference {
        val begin = 1
        val end = 5
        val sources = FileSources("first.source.domain/hello")
        val fileInfo = FileInfo("media/type", "filename", 1024)
        val fileSharingExtension = FileSharingExtension(fileInfo, sources)

        return FileReference(begin, end, fileSharingExtension)
    }

    private fun getFileReferenceWithTwoChild(): FileReference {
        val begin = 1
        val end2 = 5

        val sources1 = FileSources("first.source.domain/hello")
        val fileInfo1 = FileInfo("media/type", "filename1", 1024)
        val fileSharingExtension1 = FileSharingExtension(fileInfo1, sources1)

        val sources2 = FileSources("second.source.domain/hello")
        val fileInfo2 = FileInfo("media/anotherType", "filename2", 512)
        val fileSharingExtension2 = FileSharingExtension(fileInfo2, sources2)

        val sharingExtensionsList = listOf(fileSharingExtension1, fileSharingExtension2)

        return FileReference(begin, end2, sharingExtensionsList)
    }

    fun test_appendToXML() {
        val xmlStringBuilder1 = XmlStringBuilder()
        getFileReferenceWithOneChild().appendToXML(xmlStringBuilder1)
        val reference1 =
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
        assertEquals(reference1, xmlStringBuilder1.toString())

        val xmlStringBuilder2 = XmlStringBuilder()
        getFileReferenceWithTwoChild().appendToXML(xmlStringBuilder2)
        val reference2 =
                "<file-sharing xmlns='https://xabber.com/protocol/files'>" +
                    "<file>" +
                        "<media-type>media/type</media-type>" +
                        "<name>filename1</name>" +
                        "<size>1024</size>" +
                    "</file>" +
                    "<sources>" +
                        "<uri>first.source.domain/hello</uri>" +
                    "</sources>" +
                "</file-sharing>" +
                "<file-sharing xmlns='https://xabber.com/protocol/files'>" +
                    "<file>" +
                        "<media-type>media/anotherType</media-type>" +
                        "<name>filename2</name>" +
                        "<size>512</size>" +
                    "</file>" +
                    "<sources>" +
                        "<uri>second.source.domain/hello</uri>" +
                    "</sources>" +
                "</file-sharing>"
        assertEquals(reference2, xmlStringBuilder2.toString())
    }

}