package com.xabber.android.data.message.chat.groupchat

import com.xabber.android.R
import com.xabber.android.data.Application

enum class GroupchatMembershipType(val type: String) {
    NONE(Application.getInstance().baseContext.getString(R.string.groupchat_membership_type_none)),
    OPEN(Application.getInstance().baseContext.getString(R.string.groupchat_membership_type_open)),
    MEMBER_ONLY(Application.getInstance().baseContext.getString(R.string.groupchat_membership_type_members_only));

    fun toXml(): String? {
        return when(this) {
            OPEN -> "open"
            MEMBER_ONLY -> "member-only"
            NONE -> null
        }
    }

    override fun toString(): String = type

    companion object {
        @JvmStatic
        fun getMembershipTypeFromXml(text: String?): GroupchatMembershipType {
            return when (text) {
                "open" -> OPEN
                "member-only" -> MEMBER_ONLY
                else -> NONE
            }
        }
    }
}