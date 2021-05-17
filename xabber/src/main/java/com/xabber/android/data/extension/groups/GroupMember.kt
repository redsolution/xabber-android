package com.xabber.android.data.extension.groups

data class GroupMember(
    val id: String,
    var jid: String? = null,
    var groupJid: String? = null,
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
        get() = nickname ?: jid ?: id
}