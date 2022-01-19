package com.xabber.android.data.util

import com.xabber.android.data.dto.EmojiTypeDto

fun List<EmojiTypeDto>.toMap(): Map<String, List<List<String>>> {
    val map = this.map {
        it.name to it.list
    }.toMap()

    return map
}