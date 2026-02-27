package com.xabber.android.data.extension.iqlast

import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager.getAccount
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.roster.RosterManager
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smackx.iqlast.packet.LastActivity
import java.util.*

object LastActivityManager {

    private val lastActivities = HashMap<ContactJid, Long>()
    private val activeRequests = mutableSetOf<ContactJid>()

    fun getOrRequestLastActivity(account: AccountJid, user: ContactJid) =
        if (lastActivities.containsKey(user)) {
            lastActivities[user] ?: 0
        } else {
            if (!activeRequests.contains(user)) {
                requestLastActivityAsync(account, user)
            }
            0
        }

    private fun requestLastActivityAsync(account: AccountJid, user: ContactJid) {
        activeRequests.add(user)
        Application.getInstance().runInBackground {
            getAccount(account)?.connection?.let { connection ->
                try {
                    connection.sendIqWithResponseCallback(LastActivity(user.jid)) { response ->
                        (response as? LastActivity)?.let {
                            setLastActivity(account, user, it.lastActivity)
                            activeRequests.remove(user)
                        }
                    }
                } catch (e: NotConnectedException) {
                    LogManager.d(LastActivityManager::class.java, e.toString())
                } catch (e: InterruptedException) {
                    LogManager.d(LastActivityManager::class.java, e.toString())
                }
            }
        }
    }

    fun setLastActivity(account: AccountJid, user: ContactJid, time: Long) {
        lastActivities[user] = System.currentTimeMillis() - time * 1000
        RosterManager.onContactChanged(account, user)
    }

}