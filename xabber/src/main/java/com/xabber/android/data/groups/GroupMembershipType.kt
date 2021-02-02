package com.xabber.android.data.groups

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
        fun getMembershipTypeFromXml(text: String?): GroupMembershipType {
            return when (text) {
                "open" -> OPEN
                "member-only" -> MEMBER_ONLY
                else -> NONE
            }
        }

        @JvmStatic
        fun getMembershipByLocalizedString(text: String?): GroupMembershipType {
            return when (text) {
                Application.getInstance().applicationContext.getString(R.string.groupchat_membership_type_open) -> OPEN
                Application.getInstance().applicationContext.getString(R.string.groupchat_membership_type_members_only) -> MEMBER_ONLY
                else -> NONE
            }
        }

        @JvmStatic
        fun getLocalizedValues() : List<String>{
            val result = mutableListOf<String>()
            for (type in values())
                result.add(type.getLocalizedString())
            return result
        }
    }
}