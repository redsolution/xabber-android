package com.xabber.android.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.DialogFragment
import com.xabber.android.R
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.xaccount.XabberAccountManager

class AccountDeleteDialog : DialogFragment(), DialogInterface.OnClickListener {

    private var account: AccountJid? = null
    private var chbDeleteSettings: CheckBox? = null
    private var jid: String? = null

    var listener: DialogInterface.OnClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.getParcelable<AccountJid>(ARGUMENT_ACCOUNT)?.let {
            account = it
            jid = it.bareJid.toString()
        } ?: throw IllegalArgumentException("${this.javaClass.simpleName} needs valid AccountJid!")

        val checkBoxView =
            activity?.layoutInflater?.inflate(R.layout.dialog_delete_account, null)?.apply {
                findViewById<CheckBox>(R.id.chbDeleteSettings)?.apply {
                    isChecked = XabberAccountManager.getInstance().isAccountSynchronize(jid)
                    visibility =
                        if (XabberAccountManager.getInstance().getAccountSyncState(jid) == null) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                }.also {
                    chbDeleteSettings = it
                }
            }

        val dialogText = StringBuilder().apply {
            account?.let {
                append(
                    getString(
                        R.string.account_delete_confirmation_question,
                        AccountManager.getVerboseName(it)
                    )
                )
            }

            append(getString(R.string.account_delete_confirmation_explanation))
        }

        return AlertDialog.Builder(activity)
            .setMessage(dialogText)
            .setView(checkBoxView)
            .setPositiveButton(R.string.account_delete, this)
            .setNegativeButton(android.R.string.cancel, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which != Dialog.BUTTON_POSITIVE) return

        account?.let { AccountManager.removeAccount(it) }

        if (chbDeleteSettings != null && chbDeleteSettings?.isChecked == true) {
            XabberAccountManager.getInstance().deleteAccountSettings(jid)
        }

        listener?.onClick(dialog, which)
    }

    companion object {
        private const val ARGUMENT_ACCOUNT =
            "com.xabber.android.ui.dialog.AccountDeleteDialog.ARGUMENT_ACCOUNT"

        fun newInstance(account: AccountJid) =
            AccountDeleteDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGUMENT_ACCOUNT, account)
                }
            }
    }

}