package com.xabber.android.data.message.chat.groupchat

import com.xabber.android.R
import com.xabber.android.data.Application

enum class GroupchatPrivacyType(val string: String) {
    NONE(Application.getInstance().applicationContext.getString(R.string.groupchat_privacy_type_none)),
    INCOGNITO(Application.getInstance().applicationContext.getString(R.string.groupchat_privacy_type_incognito)),
    PUBLIC(Application.getInstance().applicationContext.getString(R.string.groupchat_privacy_type_public));

    fun toXml(): String? {
        return when(this) {
            PUBLIC -> "public"
            INCOGNITO -> "incognito"
            NONE -> null
        }
    }

    companion object {
        @JvmStatic
        fun getPrivacyTypeFromXml(text: String?): GroupchatPrivacyType {
            return when (text) {
                "public" -> PUBLIC
                "incognito" -> INCOGNITO
                else -> NONE
            }
        }
    }
}