package com.xabber.android.data

interface OnScreenListener: BaseManagerInterface {
    enum class ScreenState{
        ON, OFF
    }

    fun onScreenStateChanged(screenState: ScreenState)

}