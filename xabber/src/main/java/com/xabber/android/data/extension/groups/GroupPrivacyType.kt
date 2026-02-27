package com.xabber.android.data.extension.groups

import com.xabber.android.R
import com.xabber.android.data.Application

enum class GroupPrivacyType {
    NONE,
    INCOGNITO,
    PUBLIC;

    fun toXml(): String {
        return when(this) {
            PUBLIC -> "public"
            INCOGNITO -> "incognito"
            NONE -> "none"
        }
    }

    fun getLocalizedString(): String{
        return when(this){
            INCOGNITO -> Application.getInstance().applicationContext.getString(R.string.groupchat_privacy_type_incognito)
            PUBLIC -> Application.getInstance().applicationContext.getString(R.string.groupchat_privacy_type_public)
            else -> Application.getInstance().applicationContext.getString(R.string.groupchat_privacy_type_none)
        }
    }

    companion object {
        @JvmStatic
        fun fromXml(text: String?) =
                when (text) {
                    "public" -> PUBLIC
                    "incognito" -> INCOGNITO
                    else -> NONE
                }

        @JvmStatic
        fun fromString(string: String?) =
                when (string){
                    INCOGNITO.toString() -> INCOGNITO
                    PUBLIC.toString() -> PUBLIC
                    else -> NONE
                }

    }

}