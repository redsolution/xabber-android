package com.xabber.xmpp.mam

import com.xabber.android.data.extension.archive.MessageArchiveManager
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
    with: String? = null,
    start: Date? = null,
    end: Date? = null,
    afterId: String? = null,
    beforeId: String? = null,
    id: String? = null,
) : DataForm(Type.submit) {

    constructor(
        with: Jid? = null,
        start: Date? = null,
    ) : this(with?.asBareJid().toString(), start)

    init {
        addField(
            FormField(FormField.FORM_TYPE).apply {
                type = FormField.Type.hidden
                addValue(MessageArchiveManager.NAMESPACE)
            }
        )

        fun addFieldsIfNeed(vararg varToValPair: Pair<String, String?>) {
            varToValPair.forEach { varToVal ->
                varToVal.second?.let {
                    addField(
                        FormField(varToVal.first).apply { addValue(it) }
                    )
                }
            }
        }

        addFieldsIfNeed(
            WITH_FIELD to with,
            AFTER_FIELD to afterId,
            BEFORE_FIELD to beforeId,
            IDS_FIELD to id,
            START_FIELD to start?.let { XmppDateTime.formatXEP0082Date(it) },
            END_FIELD to end?.let { XmppDateTime.formatXEP0082Date(it) }
        )
    }

    private companion object {
        const val WITH_FIELD = "with"
        const val START_FIELD = "start"
        const val END_FIELD = "end"
        const val BEFORE_FIELD = "before-id"
        const val AFTER_FIELD = "after-id"
        const val IDS_FIELD = "{urn:xmpp:sid:0}stanza-id"
    }

}










