package com.xabber.android.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.xabber.android.R
import com.xabber.android.data.BaseIqResultUiListener
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.retract.RetractManager.isSupported
import com.xabber.android.data.extension.retract.RetractManager.sendRetractAllMessagesRequest
import com.xabber.android.data.message.MessageManager
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.XMPPError

class ChatHistoryClearDialog : DialogFragment(), View.OnClickListener, BaseIqResultUiListener {

    private lateinit var account: AccountJid
    private lateinit var user: ContactJid

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        arguments?.let {
            account = it.getParcelable(ARGUMENT_ACCOUNT) ?: throw NullPointerException(
                "${ChatHistoryClearDialog::class.java.simpleName} needs non null account jid!"
            )

            user = it.getParcelable(ARGUMENT_USER) ?: throw NullPointerException(
                "${ChatHistoryClearDialog::class.java.simpleName} needs non null contact jid!"
            )
        }

        val view = activity?.layoutInflater?.inflate(R.layout.dialog_clear_history, null)?.apply {

            findViewById<TextView>(R.id.clear_history_confirm).text =
                Html.fromHtml(
                    getString(
                        R.string.clear_chat_history_dialog_message,
                        RosterManager.getInstance().getBestContact(account, user).name
                    )
                )

            findViewById<TextView>(R.id.clear_history_warning).text =
                getString(R.string.clear_chat_history_dialog_warning)

            findViewById<Button>(R.id.clear).setTextColor(
                ColorManager.getInstance().accountPainter.getAccountMainColor(account)
            )

            findViewById<View>(R.id.clear).setOnClickListener(this@ChatHistoryClearDialog)
            findViewById<View>(R.id.cancel_clear).setOnClickListener(this@ChatHistoryClearDialog)
        }

        return AlertDialog.Builder(activity).setTitle(R.string.clear_history).setView(view).create()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.clear) {
            ChatManager.getInstance().getChat(account, user)?.setLastActionTimestamp()

            if (isSupported(account)) {
                sendRetractAllMessagesRequest(account, user, this)
            } else {
                MessageManager.getInstance().clearHistory(account, user)
            }

        }
        dismiss()
    }

    override fun onIqError(error: XMPPError) {
        activity?.runOnUiThread {
            Toast.makeText(context, error.descriptiveText, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSend() {}
    override fun onOtherError(exception: Exception?) {}
    override fun onResult() {}
    override fun processException(exception: Exception?) {}
    override fun processStanza(packet: Stanza?) {}

    companion object {
        val ARGUMENT_ACCOUNT = ChatHistoryClearDialog::class.java.name + "ARGUMENT_ACCOUNT"
        val ARGUMENT_USER = ChatHistoryClearDialog::class.java.name + "ARGUMENT_USER"

        fun newInstance(account: AccountJid, user: ContactJid) =
            ChatHistoryClearDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGUMENT_ACCOUNT, account)
                    putParcelable(ARGUMENT_USER, user)
                }
            }
    }

}