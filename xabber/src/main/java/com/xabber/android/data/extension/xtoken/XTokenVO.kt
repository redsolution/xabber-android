package com.xabber.android.data.extension.xtoken

import com.xabber.xmpp.xtoken.IncomingNewXTokenIQ

data class XToken(val uid: String, val token: String, val expire: Long) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expire
}

fun IncomingNewXTokenIQ.getXToken() = XToken(this.uid, this.token, this.expire)