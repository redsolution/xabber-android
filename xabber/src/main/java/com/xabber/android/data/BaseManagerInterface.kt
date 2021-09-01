/*
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data

/**
 * Base interface common for the registered manager.
 *
 * @author alexander.ivanov
 */
interface BaseManagerInterface

interface OnClearListener : BaseManagerInterface {
    /**
     * Clear all local data.
     * WILL BE CALLED FROM BACKGROUND THREAD. DON'T CHANGE OR ACCESS
     * APPLICATION'S DATA HERE!
     */
    fun onClear()
}

interface OnCloseListener : BaseManagerInterface {
    /**
     * Called after service have been stopped.
     * This function will be call from UI thread.
     */
    fun onClose()
}

interface OnInitializedListener : BaseManagerInterface {
    /**
     * Called once on service start and all data were loaded.
     * Called from UI thread.
     */
    fun onInitialized()
}

interface OnLoadListener : BaseManagerInterface {
    /**
     * Called after service has been started before
     * [OnInitializedListener].
     * WILL BE CALLED FROM BACKGROUND THREAD. DON'T CHANGE OR ACCESS
     * APPLICATION'S DATA HERE!
     * Used to load data from DB and post request to UI thread to update data.
     */
    fun onLoad()
}

interface OnLowMemoryListener : BaseManagerInterface {
    /**
     * Clears all caches.
     */
    fun onLowMemory()
}

interface OnTimerListener : BaseManagerInterface {
    /**
     * Called after at least [.DELAY] milliseconds.
     */
    fun onTimer()

    companion object {
        const val DELAY = 1000
    }
}

interface OnUnloadListener : BaseManagerInterface {
    /**
     * Called before application to be killed after
     * [OnCloseListener.onClose] has been called.
     * WILL BE CALLED FROM BACKGROUND THREAD. DON'T CHANGE OR ACCESS
     * APPLICATION'S DATA HERE!
     */
    fun onUnload()
}

interface OnWipeListener : BaseManagerInterface {
    /**
     * Wipe all sensitive application data.
     * WILL BE CALLED FROM BACKGROUND THREAD. DON'T CHANGE OR ACCESS
     * APPLICATION'S DATA HERE!
     */
    fun onWipe()
}
