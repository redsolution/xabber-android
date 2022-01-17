package com.xabber.android.data.dto

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class HostListDto(
    @Expose
    @SerializedName("results")
    val list: List<HostDto>
)