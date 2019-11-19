package com.xabber.android.data.extension.utils
import org.junit.Test
import com.xabber.android.utils.*
import org.junit.Assert.*

class StringUtilsTest{

    @Test
    fun testXmlBeautifier(){
        val before : String = "<?xml version=\\\"1.0\\\"?><!-- Represents Books information in store --><books><book id=\\\"1\\\"><name>Let Us C</name><author>Yashwant Kanetkar</author><price>245.00</price></book><book id=\\\"2\\\"><name>Let Us C++</name><author>Yashwant Kanetkar</author><price>252.00</price></book><book id=\\\"3\\\"><name>Java The Complete Reference</name><author>Herbert Schildt</author><price>489.00</price></book><book id=\\\"4\\\"><name>HTML5 Black Book</name><author>Kogent Learning Solutions</author><price>485.00</price></book></books>"
        val after : String = StringUtils.getPrettyXmlString(before, 2)

        val ref =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- Represents Books information in store --><books>\n" +
                "  <book id=\"1\">\n" +
                "    <name>Let Us C</name>\n" +
                "    <author>Yashwant Kanetkar</author>\n" +
                "    <price>245.00</price>\n" +
                "  </book>\n" +
                "  <book id=\"2\">\n" +
                "    <name>Let Us C++</name>\n" +
                "    <author>Yashwant Kanetkar</author>\n" +
                "    <price>252.00</price>\n" +
                "  </book>\n" +
                "  <book id=\"3\">\n" +
                "    <name>Java The Complete Reference</name>\n" +
                "    <author>Herbert Schildt</author>\n" +
                "    <price>489.00</price>\n" +
                "  </book>\n" +
                "  <book id=\"4\">\n" +
                "    <name>HTML5 Black Book</name>\n" +
                "    <author>Kogent Learning Solutions</author>\n" +
                "    <price>485.00</price>\n" +
                "  </book>\n" +
                "</books>"
        assertEquals(after, ref)
    }
}