package com.xabber.xmpp.mam

import com.xabber.android.data.extension.mam.MessageArchiveManager
import org.jivesoftware.smackx.xdata.FormField
import org.jivesoftware.smackx.xdata.packet.DataForm
import org.jxmpp.jid.Jid
import org.jxmpp.util.XmppDateTime
import java.util.*

/**
 * Data Form Extension to be used as extension at [MamQueryIQ].
 * Possible fields are limited by XEP-0313
 */
class MamDataFormExtension(
    private val with: Jid? = null,
    private val start: Date? = null,
    private val end: Date? = null,
    private val afterId: String? = null,
    private val beforeId: String? = null,
    private val id: String? = null,
) : DataForm(Type.submit) {

    init {
        addField(
            FormField(FormField.FORM_TYPE).apply {
                type = FormField.Type.hidden
                addValue(MessageArchiveManager.NAMESPACE)
            }
        )

        if (with != null) addField(FormField(WITH_FIELD).apply { addValue(with.asBareJid().toString()) })
        if (start != null) addField(FormField(START_FIELD).apply { addValue(XmppDateTime.formatXEP0082Date(start)) })
        if (end != null) addField(FormField(END_FIELD).apply { addValue(XmppDateTime.formatXEP0082Date(end)) })
        if (afterId != null) addField(FormField(AFTER_FIELD).apply { addValue(afterId) })
        if (beforeId != null) addField(FormField(BEFORE_FIELD).apply { addValue(beforeId) })
        if (!id.isNullOrEmpty()) addField(FormField(IDS_FIELD).apply { addValue(id) })
    }

    companion object {
        private const val WITH_FIELD = "with"
        private const val START_FIELD = "start"
        private const val END_FIELD = "end"
        private const val BEFORE_FIELD = "before-id"
        private const val AFTER_FIELD = "after-id"
        private const val IDS_FIELD = "{urn:xmpp:sid:0}stanza-id"
    }

}










