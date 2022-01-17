package com.xabber.android.presentation.emoji

import com.google.gson.annotations.SerializedName

data class EmojiSet(
    @SerializedName("emojis")
    val emojis: List<List<String>>,
    @SerializedName("type")
    val type: String
)