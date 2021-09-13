package com.xabber.android.data.extension.xtoken

import com.xabber.android.ui.text.getSmartTimeTextForRoster
import com.xabber.xmpp.xtoken.ResultSessionsIQ
import java.util.*

data class SessionVO(
    val client: String,
    val device: String,
    val uid: String,
    val ip: String,
    val lastAuth: String
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
                .first { it.uid == currentSessionUid }
                .let { SessionVO(it.client, it.device, it.uid, it.ip, "online") },
            iqSessions
                .filter { it.uid != currentSessionUid }
                .map {
                    SessionVO(
                        it.client,
                        it.device,
                        it.uid,
                        it.ip,
                        Date(it.lastAuth).getSmartTimeTextForRoster()
                    )
                }
        )
    }