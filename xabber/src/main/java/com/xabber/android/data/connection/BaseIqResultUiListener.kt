package com.xabber.android.data.connection

import org.jivesoftware.smack.ExceptionCallback
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException.XMPPErrorException
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.XMPPError

interface BaseIqResultUiListener : ExceptionCallback, StanzaListener {

    fun onSend()
    fun onResult()
    fun onIqError(error: XMPPError)
    fun onOtherError(exception: Exception? = null)

    fun onIqErrors(errors: List<XMPPError>) = errors.forEach { error -> onIqError(error) }
    fun onOtherErrors(exceptions: List<java.lang.Exception>) =
        exceptions.forEach { exception -> onOtherError(exception) }

    override fun processException(exception: Exception?) =
        if (exception is XMPPErrorException)
            onIqError(exception.xmppError)
        else onOtherError(exception)

    override fun processStanza(packet: Stanza?) =
        if (packet is IQ && packet.type == IQ.Type.result)
            onResult()
        else onOtherError()

}