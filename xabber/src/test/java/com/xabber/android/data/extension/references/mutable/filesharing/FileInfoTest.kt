package com.xabber.android.data.extension.references.mutable.filesharing

import org.junit.Assert.assertEquals
import org.junit.Test

class FileInfoTest {

    @Test
    fun test_toXml(){
        val mediaType = "image/jpeg"
        val fileName = "file name"
        val fileSize: Long = 256

        val fileInfo = FileInfo(mediaType, fileName, fileSize)

        val reference1 =
                        "<file>" +
                            "<media-type>image/jpeg</media-type>" +
                            "<name>file name</name>" +
                            "<size>256</size>" +
                        "</file>"
        assertEquals(reference1, fileInfo.toXML().toString())

        fileInfo.width = 128
        fileInfo.height = 128
        val reference2 =
                        "<file>" +
                            "<media-type>image/jpeg</media-type>" +
                            "<name>file name</name>" +
                            "<size>256</size>" +
                            "<height>128</height>" +
                            "<width>128</width>" +
                        "</file>"
        assertEquals(reference2, fileInfo.toXML().toString())

        fileInfo.duration = 512
        fileInfo.desc = "Description of file"
        val reference3 =
                        "<file>" +
                            "<media-type>image/jpeg</media-type>" +
                            "<name>file name</name>" +
                            "<size>256</size>" +
                            "<height>128</height>" +
                            "<width>128</width>" +
                            "<desc>Description of file</desc>" +
                            "<duration>512</duration>" +
                        "</file>"
        assertEquals(reference3, fileInfo.toXML().toString())

        fileInfo.desc = ""
        fileInfo.width = 0
        val reference4 =
                        "<file>" +
                            "<media-type>image/jpeg</media-type>" +
                            "<name>file name</name>" +
                            "<size>256</size>" +
                            "<height>128</height>" +
                            "<duration>512</duration>" +
                            "</file>"
        assertEquals(reference4, fileInfo.toXML().toString())
    }

}