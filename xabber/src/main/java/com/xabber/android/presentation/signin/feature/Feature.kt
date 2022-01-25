package com.xabber.android.presentation.signin.feature

import com.xabber.android.presentation.signin.feature.State.Loading

class Feature(
    val nameResId: Int,
    var state: State = Loading
)

enum class State {
    Loading,
    Success,
    Error
}