package com.xabber.android.data.extension.groups

import com.xabber.android.R
import com.xabber.android.data.Application

enum class GroupMembershipType {
    NONE,
    OPEN,
    MEMBER_ONLY;

    fun toXml(): String {
        return when(this) {
            OPEN -> "open"
            MEMBER_ONLY -> "member-only"
            NONE -> "none"
        }
    }

    fun getLocalizedString(): String{
        return  when(this){
            OPEN -> Application.getInstance().applicationContext.getString(R.string.groupchat_membership_type_open)
            MEMBER_ONLY -> Application.getInstance().applicationContext.getString(R.string.groupchat_membership_type_members_only)
            else -> Application.getInstance().applicationContext.getString(R.string.groupchat_membership_type_none)
        }
    }

    companion object {
        @JvmStatic
        fun fromXml(text: String?)=
                when (text) {
                    "open" -> OPEN
                    "member-only" -> MEMBER_ONLY
                    else -> NONE
                }


        @JvmStatic
        fun fromString(string: String?) =
                when (string){
                    OPEN.toString() -> OPEN
                    MEMBER_ONLY.toString() -> MEMBER_ONLY
                    else -> NONE
                }

    }

}