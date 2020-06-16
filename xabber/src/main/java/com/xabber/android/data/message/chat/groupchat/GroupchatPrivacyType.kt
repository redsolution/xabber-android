package com.xabber.android.data.message.chat.groupchat

enum class GroupchatPrivacyType {
    none,
    incognitoGroupChat,
    publicGroupChat;

    fun toXml(): String? {
        return when(this) {
            publicGroupChat -> "public"
            incognitoGroupChat -> "incognito"
            none -> null
        }
    }

    companion object {
        @JvmStatic
        fun getPrivacyTypeFromXml(text: String?): GroupchatPrivacyType {
            return when (text) {
                "public" -> publicGroupChat
                "incognito" -> incognitoGroupChat
                else -> none
            }
        }
    }
}