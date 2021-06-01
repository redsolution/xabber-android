package com.xabber.android.data.message

import com.xabber.android.data.Application
import org.jivesoftware.smack.packet.Message
import java.util.*

object MessageUtils {

    /**
     * Parse message bodies to find out most suitable.
     * Priority order:
     * 1) If message doesn't contain a body, returns null;
     * 2) If message contains only one body, returns it;
     * 3) If message contains body with current system locale language, returns it;
     * 4) If message contains body with english or US, returns it;
     * 5) If above conditions aren't met, returns first body;
     */
    @JvmStatic
    fun getOptimalTextBody(message: Message): String? {

        if (message.body != null) return message.body
        return if (message.bodies.size != 0) {

            val bodies = message.bodies
            if (bodies.size == 1) {
                return (bodies.toTypedArray()[0] as Message.Body).message
            }

            val currentLocale = Application.getInstance().resources.configuration.locale.toString()
            for (body in bodies) if (body.language == currentLocale) return body.message

            val englishLocale = Locale.getDefault().language.toLowerCase(Locale.ENGLISH)
            val usLocale = Locale.getDefault().language.toLowerCase(Locale.US)
            for (body in bodies) {
                if (body.language == englishLocale || body.language == usLocale) return body.message
            }

            (bodies.toTypedArray()[0] as Message.Body).message

        } else null
    }
}