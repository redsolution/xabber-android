package com.xabber.android.data.message.chat.groupchat

import com.xabber.android.R
import com.xabber.android.data.Application

enum class GroupchatIndexType(val type: String) {
    NONE(Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_none)),       // explicit "none"
    GLOBAL(Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_global)),     // explicit "global"
    LOCAL(Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_local)),      // explicit "local"
    NONE_AS_NULL(Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_none)); // implicit "null" value, i.e. <index> wasn't found/saved

    fun toXml(): String? {
        return when(this) {
            LOCAL -> "local"
            GLOBAL -> "global"
            NONE -> "none"
            else -> null
        }
    }

    override fun toString(): String = type

    companion object {
        @JvmStatic
        fun getPrivacyTypeFromXml(text: String?): GroupchatIndexType {
            return when (text) {
                "local" -> LOCAL
                "global" -> GLOBAL
                "none" -> NONE
                else -> NONE_AS_NULL
            }
        }
    }
}