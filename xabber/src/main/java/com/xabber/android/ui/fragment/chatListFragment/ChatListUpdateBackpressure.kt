package com.xabber.android.ui.fragment.chatListFragment

import android.os.Handler
import java.util.*

class ChatListUpdateBackpressure(private var updatableObject: UpdatableObject?) : Runnable {

    private val REFRESH_INTERVAL: Long = 2000

    interface UpdatableObject {
        fun update()
    }

    /**
     * Handler for deferred refresh.
     */
    private var handler: Handler? = null

    /**
     * Lock for refresh requests.
     */
    private var refreshLock: Any? = null

    /**
     * Whether refresh was requested.
     */
    private var refreshRequested = false

    /**
     * Whether refresh is in progress.
     */
    private var refreshInProgress = false

    /**
     * Minimal time when next refresh can be executed.
     */
    private var nextRefresh: Date? = null

    init {
        handler = Handler()
        refreshLock = Any()
        refreshRequested = false
        refreshInProgress = false
        nextRefresh = Date()
    }

    override fun run() {
        build()
    }

    /**
     * Requests refresh in some time in future.
     */
    fun refreshRequest() {
        synchronized(refreshLock!!) {
            if (refreshRequested) {
                return
            }
            if (refreshInProgress) {
                refreshRequested = true
            } else {
                val delay = nextRefresh!!.time - Date().time
                handler!!.postDelayed(this, if (delay > 0) delay else 0)
            }
        }
    }

    /**
     * Remove refresh requests.
     */
    fun removeRefreshRequests() {
        synchronized(refreshLock!!) {
            refreshRequested = false
            refreshInProgress = false
            handler!!.removeCallbacks(this)
        }
    }

    fun build() {
        synchronized(refreshLock!!) {
            refreshRequested = false
            refreshInProgress = true
            handler!!.removeCallbacks(this)
        }
        updatableObject!!.update()
        synchronized(refreshLock!!) {
            nextRefresh = Date(Date().time + REFRESH_INTERVAL)
            refreshInProgress = false
            handler!!.removeCallbacks(this) // Just to be sure.
            if (refreshRequested) {
                handler!!.postDelayed(this, REFRESH_INTERVAL)
            }
        }
    }
}