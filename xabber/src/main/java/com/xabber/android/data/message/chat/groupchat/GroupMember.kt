package com.xabber.android.data.message.chat.groupchat

data class GroupMember(val id: String) {
    var jid: String? = null
    var groupchatJid: String? = null
    var nickname: String? = null
    var role: String? = null
    var badge: String? = null
    var avatarHash: String? = null
    var avatarUrl: String? = null
    var lastPresent: String? = null
    var isMe = false
    var isBlocked = false
    var isKicked = false
    var isCanRestrictMembers = false
    var isCanBlockMembers = false
    var isCanChangeBadge = false
    var isCanChangeNickname = false
    var isCanDeleteMessages = false
    var isRestrictedToSendMessages = false
    var isRestrictedToReadMessages = false
    var isRestrictedToSendInvitations = false
    var isRestrictedToSendAudio = false
    var isRestrictedToSendImages = false

    constructor(id: String, jid: String?, groupchatJid: String?, nickname: String?, role: String?, badge: String?,
                avatarHash: String?, avatarUrl: String?, lastPresent: String?, isMe: Boolean,
                isCanRestrictMembers: Boolean, isCanBlockMembers: Boolean, isCanChangeBadge: Boolean,
                isCanChangeNickname: Boolean, isCanDeleteMessages: Boolean, isRestrictedToSendMessages: Boolean,
                isRestrictedToReadMessages: Boolean, isRestrictedToSendInvitations: Boolean,
                isRestrictedToSendAudio: Boolean, isRestrictedToSendImages: Boolean) : this(id) {
        this.jid = jid
        this.groupchatJid = groupchatJid
        this.nickname = nickname
        this.role = role
        this.badge = badge
        this.avatarHash = avatarHash
        this.avatarUrl = avatarUrl
        this.lastPresent = lastPresent
        this.isMe = isMe
        this.isCanRestrictMembers = isCanRestrictMembers
        this.isCanBlockMembers = isCanBlockMembers
        this.isCanChangeBadge = isCanChangeBadge
        this.isCanChangeNickname = isCanChangeNickname
        this.isCanDeleteMessages = isCanDeleteMessages
        this.isRestrictedToSendMessages = isRestrictedToSendMessages
        this.isRestrictedToReadMessages = isRestrictedToReadMessages
        this.isRestrictedToSendInvitations = isRestrictedToSendInvitations
        this.isRestrictedToSendAudio = isRestrictedToSendAudio
        this.isRestrictedToSendImages = isRestrictedToSendImages
    }

    val bestName: String
        get() = if (nickname != null && nickname!!.isNotEmpty()) {
            nickname!!
        } else if (jid != null && jid!!.isNotEmpty()) {
            jid!!
        } else {
            id
        }

}