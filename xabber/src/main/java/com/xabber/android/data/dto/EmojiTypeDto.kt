package com.xabber.android.data.dto

import com.google.gson.annotations.SerializedName

data class EmojiTypeDto(
    @SerializedName("emojis")
    val list: List<List<String>>,
    @SerializedName("type")
    val name: String
)