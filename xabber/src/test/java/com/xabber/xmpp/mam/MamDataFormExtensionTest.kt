package com.xabber.xmpp.mam

import com.xabber.android.data.entity.ContactJid
import junit.framework.TestCase
import org.junit.Test
import java.util.*

class MamDataFormExtensionTest : TestCase() {

    @Test
    fun test_toXml() {
        val reference1 = "<x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' " +
                "type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>hello@domain.com</value></field><field var='ids'><value>firstStanzaId</value></field></x>"
        val dataForm1 = MamDataFormExtension(
            id = "firstStanzaId",
            with = ContactJid.from("hello@domain.com").bareJid,
        )

//        val reference2 = "<x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='after-id'><value>afterId</value></field><field var='ids'><value>firstId</value><value>secondId</value><value>anotherThirdId</value></field></x>"
//        val dataForm2 = MamDataFormExtension(
//                id = listOf("firstId", "secondId", "anotherThirdId"),
//                afterId = "afterId",
//        )

        val reference3 =
            "<x xmlns='jabber:x:data' type='submit'><field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:2</value></field><field var='with'><value>contact@server.domain</value></field><field var='start'><value>2019-02-12T19:33:20.000+00:00</value></field><field var='end'><value>2019-06-08T13:20:00.000+00:00</value></field></x>"
        val dataform3 = MamDataFormExtension(
            with = ContactJid.from("contact@server.domain").bareJid,
            start = Date(1550000000000),
            end = Date(1560000000000),
        )

        assertEquals(reference1, dataForm1.toXML().toString())
        //assertEquals(reference2, dataForm2.toXML().toString())
        assertEquals(reference3, dataform3.toXML().toString())
    }

}