package com.xabber.android.data.extension.devices

import android.os.Parcel
import android.os.Parcelable
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.ui.text.getSmartTimeTextForRoster
import com.xabber.xmpp.devices.DeviceExtensionElement
import com.xabber.xmpp.devices.ResultSessionsIQ
import java.util.*

data class SessionVO(
    val client: String,
    val device: String,
    val uid: String,
    val ip: String,
    val lastAuth: String,
    val expire: String,
    val description: String?,
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeStringList(
            listOf(client, device, uid, ip, lastAuth, expire, description)
        )
    }

    companion object CREATOR : Parcelable.Creator<SessionVO> {
        override fun createFromParcel(parcel: Parcel): SessionVO {
            return SessionVO(parcel)
        }

        override fun newArray(size: Int): Array<SessionVO?> {
            return arrayOfNulls(size)
        }
    }

    fun createSmartLastSeen(accountJid: AccountJid, presenceManager: PresenceManager): String {
        val hasAvailablePresence = presenceManager.getAvailableAccountPresences(accountJid)
            .filter { it.hasExtension(DevicesManager.NAMESPACE) && it.isAvailable }
            .mapNotNull {
                it.getExtension(DeviceExtensionElement.NAMESPACE) as DeviceExtensionElement?
            }.any { it.deviceId == uid }

        return if (hasAvailablePresence) "Online" else lastAuth
    }

}

/**
 * First element of pair is current session
 * Second element of pair is lost of other sessions
 */
fun ResultSessionsIQ.getMainAndOtherSessions(
    currentSessionUid: String
): Pair<SessionVO, List<SessionVO>> =
    this.sessions.sortedByDescending { it.lastAuth }.let { iqSessions ->
        Pair(
            iqSessions.first { it.id == currentSessionUid }.let {
                SessionVO(
                    it.client, it.info, it.id, it.ip, "online", it.expire.toString(), it.description
                )
            },
            iqSessions.filter { it.id != currentSessionUid }.map {
                SessionVO(
                    it.client,
                    it.info,
                    it.id,
                    it.ip,
                    Date(it.lastAuth).getSmartTimeTextForRoster(),
                    it.expire.toString(),
                    it.description
                )
            }
        )
    }