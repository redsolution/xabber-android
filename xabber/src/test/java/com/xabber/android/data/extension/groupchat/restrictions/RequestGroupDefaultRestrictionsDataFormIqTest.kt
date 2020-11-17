package com.xabber.android.data.extension.groupchat.restrictions

import com.xabber.android.data.entity.ContactJid
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class RequestGroupDefaultRestrictionsDataFormIqTest {

    private lateinit var groupJid: ContactJid
    private lateinit var iq: RequestGroupDefaultRestrictionsDataFormIQ

    private val stringReference = ""

    @Before
    fun setup(){
        groupJid = Mockito.mock(ContactJid::class.java)

        iq = RequestGroupDefaultRestrictionsDataFormIQ(groupJid)
    }

    @Test
    fun test_getIQChildElementBuilder(){
        Assert.assertEquals(iq.toXML().toString(), stringReference)
    }

}