package com.xabber.android.data.dto

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class HostDto(
    @Expose
    @SerializedName("host")
    val name: String
)