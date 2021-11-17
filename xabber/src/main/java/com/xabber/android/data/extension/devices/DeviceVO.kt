package com.xabber.android.data.extension.devices

import android.util.Base64
import com.xabber.android.data.log.LogManager
import com.xabber.xmpp.devices.IncomingNewDeviceIQ
import com.xabber.xmpp.smack.HmacPasswordGenerator
import javax.crypto.spec.SecretKeySpec

data class DeviceVO(val uid: String, val secret: String, val expire: Long, var counter: Int = 1) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expire

    private val decodedSecret = Base64.decode(secret, Base64.DEFAULT)
    fun getPasswordString(): String {
        val result = HmacPasswordGenerator().generateOneTimePassword(
            SecretKeySpec(decodedSecret, "HmacSHA1"),
            counter.toLong()
        )
        LogManager.d("XTOKEN", "getPasswordString() with secret: $secret and counter: $counter; and result is: $result")
        return result
    }
}

fun IncomingNewDeviceIQ.getDeviceElement() = DeviceVO(this.uid, this.secret, this.expire)