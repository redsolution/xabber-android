package com.xabber.android.data.dto

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.jxmpp.jid.DomainBareJid
import org.jxmpp.jid.FullJid
import org.jxmpp.jid.parts.Domainpart
import org.jxmpp.jid.parts.Localpart

data class XabberAccountDto(
    @Expose
    @SerializedName("username")
    val username: Localpart,
    @Expose
    @SerializedName("domain")
    val domain: DomainBareJid,
    @Expose
    @SerializedName("full_id")
    val full_id: FullJid,
)