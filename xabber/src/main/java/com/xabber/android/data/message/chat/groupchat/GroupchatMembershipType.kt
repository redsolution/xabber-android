package com.xabber.android.data.message.chat.groupchat

enum class GroupchatMembershipType {
    none,
    open,
    memberOnly;

    fun toXml(): String? {
        return when(this) {
            open -> "open"
            memberOnly -> "member-only"
            none -> null
        }
    }

    companion object {

        @JvmStatic
        fun getMembershipTypeFromXml(text: String?): GroupchatMembershipType {
            return when (text) {
                "open" -> open
                "member-only" -> memberOnly
                else -> none
            }
        }
    }
}