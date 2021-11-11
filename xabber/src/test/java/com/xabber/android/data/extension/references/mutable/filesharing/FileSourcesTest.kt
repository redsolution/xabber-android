package com.xabber.android.data.extension.references.mutable.filesharing

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSourcesTest {

    @Test
    fun test_toXml(){
        val fileSources1 = FileSources("https://picsum.photos/200")
        val reference1 =
                        "<sources>" +
                            "<uri>https://picsum.photos/200</uri>" +
                        "</sources>"
        assertEquals(reference1, fileSources1.toXML().toString())

        val fileSources2 = FileSources(listOf("https://picsum.photos/100", "https://picsum.photos/200",
                "https://picsum.photos/300"))
        val reference2 =
                        "<sources>" +
                            "<uri>https://picsum.photos/100</uri>" +
                            "<uri>https://picsum.photos/200</uri>" +
                            "<uri>https://picsum.photos/300</uri>" +
                        "</sources>"
        assertEquals(reference2, fileSources2.toXML().toString())
    }

}