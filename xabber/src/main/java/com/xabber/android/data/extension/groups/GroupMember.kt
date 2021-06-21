package com.xabber.android.data.extension.groups

import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid

data class GroupMember(
    val primaryKey: String,

    val memberId: String,
    val accountJid: AccountJid,
    val groupJid: ContactJid,

    var jid: String? = null,
    var nickname: String? = null,
    var role: String? = null,
    var badge: String? = null,
    var avatarHash: String? = null,
    var avatarUrl: String? = null,
    var lastPresent: String? = null,
    var isMe: Boolean = false,
    var isBlocked: Boolean = false,
    var isKicked: Boolean = false,
) {
    val bestName: String
        get() = nickname ?: jid ?: memberId
}