package com.xabber.android.data.extension.devices

import com.xabber.android.ui.text.getSmartTimeTextForRoster
import com.xabber.xmpp.devices.ResultSessionsIQ
import java.util.*

data class SessionVO(
    val client: String,
    val device: String,
    val uid: String,
    val ip: String,
    val lastAuth: String,
    val description: String?,
)

/**
 * First element of pair is current session
 * Second element of pair is lost of other sessions
 */
fun ResultSessionsIQ.getMainAndOtherSessions(
    currentSessionUid: String
): Pair<SessionVO, List<SessionVO>> =
    this.sessions.sortedByDescending { it.lastAuth }.let { iqSessions ->
        Pair(
            iqSessions
                .first { it.id == currentSessionUid }
                .let { SessionVO(it.client, it.info, it.id, it.ip, "online", it.description) },
            iqSessions
                .filter { it.id != currentSessionUid }
                .map {
                    SessionVO(
                        it.client,
                        it.info,
                        it.id,
                        it.ip,
                        Date(it.lastAuth).getSmartTimeTextForRoster(),
                        it.description
                    )
                }
        )
    }