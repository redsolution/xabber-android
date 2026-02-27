package com.xabber.xmpp.smack

import org.junit.Assert
import org.junit.Test
import java.security.Key
import javax.crypto.spec.SecretKeySpec

class HmacPasswordGeneratorTest {

    @Test
    fun testGenerateOneTimePassword() {
        val key: Key = SecretKeySpec("12345678901234567890".toByteArray(), "HmacSHA1")
        Assert.assertEquals("84755224", HmacPasswordGenerator().generateOneTimePassword(key, 0))
        Assert.assertEquals("94287082", HmacPasswordGenerator().generateOneTimePassword(key, 1))
        Assert.assertEquals("37359152", HmacPasswordGenerator().generateOneTimePassword(key, 2))
        Assert.assertEquals("26969429", HmacPasswordGenerator().generateOneTimePassword(key, 3))
        Assert.assertEquals("40338314", HmacPasswordGenerator().generateOneTimePassword(key, 4))
        Assert.assertEquals("68254676", HmacPasswordGenerator().generateOneTimePassword(key, 5))
        Assert.assertEquals("18287922", HmacPasswordGenerator().generateOneTimePassword(key, 6))
        Assert.assertEquals("82162583", HmacPasswordGenerator().generateOneTimePassword(key, 7))
        Assert.assertEquals("73399871", HmacPasswordGenerator().generateOneTimePassword(key, 8))
        Assert.assertEquals("45520489", HmacPasswordGenerator().generateOneTimePassword(key, 9))
    }

}