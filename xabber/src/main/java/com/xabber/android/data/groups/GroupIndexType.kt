package com.xabber.android.data.groups

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
        fun getIndexTypeFromXml(text: String?): GroupIndexType {
            return when (text) {
                "local" -> LOCAL
                "global" -> GLOBAL
                else -> NONE
            }
        }

        @JvmStatic
        fun getIndexTypeByLocalizedString(text: String?) : GroupIndexType {
            return when (text) {
                Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_local) -> LOCAL
                Application.getInstance().applicationContext.getString(R.string.groupchat_index_type_global) -> GLOBAL
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