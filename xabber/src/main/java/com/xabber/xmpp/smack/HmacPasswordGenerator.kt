package com.xabber.xmpp.smack

import java.nio.ByteBuffer
import java.security.Key
import javax.crypto.Mac
import javax.crypto.ShortBufferException
import kotlin.experimental.and

open class HmacPasswordGenerator(algorithm: String = HOTP_HMAC_ALGORITHM) {

    private val mac: Mac = Mac.getInstance(algorithm)

    private val buffer: ByteBuffer = ByteBuffer.allocate(mac.macLength)

    @Synchronized
    fun generateOneTimePassword(key: Key?, counter: Long): String {
        buffer.clear()
        buffer.putLong(0, counter)
        try {
            val array: ByteArray = buffer.array()
            mac.init(key)
            mac.update(array, 0, 8)
            mac.doFinal(array, 0)
        } catch (e: ShortBufferException) {
            throw RuntimeException(e)
        }
        val offset: Byte = buffer[buffer.capacity() - 1] and 0x0f
        val truncated = (buffer.getInt(offset.toInt()) and 0x7fffffff) % MOD_DIV
        return String.format("%08d", truncated)
    }

    private companion object {
        const val MOD_DIV = 100000000 //truncate to 8 symbols length
        const val HOTP_HMAC_ALGORITHM = "HmacSHA1"
    }
}