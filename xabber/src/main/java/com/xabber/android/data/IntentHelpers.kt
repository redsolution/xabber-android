package com.xabber.android.data

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

private const val INTENT_ACCOUNT_JID_KEY = "com.xabber.android.data.INTENT_ACCOUNT_JID_KEY"
private const val INTENT_CONTACT_JID_KEY = "com.xabber.android.data.INTENT_CONTACT_JID_KEY"

fun createAccountIntent(context: Context, component: Class<*>, accountJid: AccountJid) =
    Intent(context, component).apply {
        putExtra(INTENT_ACCOUNT_JID_KEY, accountJid as Parcelable)
    }

fun createContactIntent(
    context: Context, component: Class<*>, accountJid: AccountJid, contactJid: ContactJid
) = createAccountIntent(context, component, accountJid).apply {
    putExtra(INTENT_CONTACT_JID_KEY, contactJid)
}

fun Intent.getAccountJid(): AccountJid? = this.getParcelableExtra(INTENT_ACCOUNT_JID_KEY)

fun Intent.getContactJid(): ContactJid? = this.getParcelableExtra(INTENT_CONTACT_JID_KEY)