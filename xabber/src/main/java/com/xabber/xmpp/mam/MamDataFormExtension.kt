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
    ids: List<String>? = null,
) : DataForm(Type.submit) {

    init {
        addField(
            FormField(FormField.FORM_TYPE).apply {
                type = FormField.Type.hidden
                addValue(MessageArchiveManager.NAMESPACE)
            }
        )

        fun addFieldIfNeed(variable: String, value: String?) {
            value?.let {
                addField(
                    FormField(variable).apply {
                        addValue(it)
                    }
                )
            }
        }

        fun addFieldIfNeed(variable: String, values: List<String>?) {
            values
                ?.takeIf { it.isNotEmpty() }
                ?.let {
                    addField(
                        FormField(variable).apply {
                            addValues(it)
                        }
                    )
                }
        }

        fun addFieldsIfNeed(vararg varToValPair: Pair<String, String?>) {
            varToValPair.forEach { (variable, value) -> addFieldIfNeed(variable, value) }
        }

        addFieldsIfNeed(
            WITH_FIELD to with,
            AFTER_FIELD to afterId,
            BEFORE_FIELD to beforeId,
            START_FIELD to start?.let { XmppDateTime.formatXEP0082Date(it) },
            END_FIELD to end?.let { XmppDateTime.formatXEP0082Date(it) }
        )

        addFieldIfNeed(IDS_FIELD, ids)
    }

    private companion object {
        const val WITH_FIELD = "with"
        const val START_FIELD = "start"
        const val END_FIELD = "end"
        const val BEFORE_FIELD = "before-id"
        const val AFTER_FIELD = "after-id"
        const val IDS_FIELD = "ids"
    }

}










