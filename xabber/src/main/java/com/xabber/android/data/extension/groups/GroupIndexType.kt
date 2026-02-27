package com.xabber.android.data.extension.groups

import com.xabber.android.R
import com.xabber.android.data.Application

enum class GroupIndexType{
    NONE,       // explicit "none"
    GLOBAL,     // explicit "global"
    LOCAL;      // explicit "local"

    fun toXml(): String {
        return when(this) {
            LOCAL -> "local"
            GLOBAL -> "global"
            else -> "none"
        }
    }

    fun getLocalizedString(): String{
        return when(this){
            GLOBAL -> Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_global)
            LOCAL -> Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_local)
            else -> Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_none)
        }
    }

    companion object {
        @JvmStatic
        fun fromXml(text: String?) =
                when (text) {
                    "local" -> LOCAL
                    "global" -> GLOBAL
                    else -> NONE
                }


        @JvmStatic
        fun fromString(string: String?) =
                when (string){
                    GLOBAL.toString() -> GLOBAL
                    LOCAL.toString() -> LOCAL
                    else -> NONE
                }
    }

}