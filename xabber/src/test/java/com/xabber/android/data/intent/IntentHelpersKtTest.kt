package com.xabber.android.data.intent

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.xabber.android.data.createAccountIntent
import com.xabber.android.data.createContactIntent
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.getAccountJid
import com.xabber.android.data.getContactJid
import junit.framework.TestCase
import org.mockito.Mockito

class IntentHelpersKtTest : TestCase() {

    private val context = Mockito.mock(Context::class.java)
    private val accountJid = AccountJid.from("test@account.jid/res")
    private val contactJid = ContactJid.from("user@contact.jid")

    private val accountIntent = createAccountIntent(context, Activity::class.java, accountJid)
    private val contactIntent =
        createContactIntent(context, Activity::class.java, accountJid, contactJid)

    private val intentNullExtra = Intent(context, Activity::class.java)

    fun testCreateAndGetAccountIntent() {
        assertEquals(accountJid.toString(), accountIntent.getAccountJid().toString())
    }

    fun testCreateContactIntent() {
        assertEquals(contactJid.toString(), contactIntent.getContactJid().toString())
    }

    fun testGetAccountJid() {
        assertNull(intentNullExtra.getAccountJid())
    }

    fun testGetContactJid() {
        assertNull(intentNullExtra.getContactJid())
    }

}