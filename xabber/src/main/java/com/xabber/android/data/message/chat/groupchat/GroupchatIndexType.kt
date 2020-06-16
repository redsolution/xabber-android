package com.xabber.android.data.message.chat.groupchat

enum class GroupchatIndexType {
    none,       // explicit "none"
    global,     // explicit "global"
    local,      // explicit "local"
    noneAsNull; // implicit "null" value, i.e. <index> wasn't found/saved

    fun toXml(): String? {
        return when(this) {
            local -> "local"
            global -> "global"
            none -> "none"
            else -> null
        }
    }

    companion object {
        @JvmStatic
        fun getPrivacyTypeFromXml(text: String?): GroupchatIndexType {
            return when (text) {
                "local" -> local
                "global" -> global
                "none" -> none
                else -> noneAsNull
            }
        }
    }
}