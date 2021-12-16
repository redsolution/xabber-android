package com.xabber.android.ui.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.DrawableRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.extension.devices.DevicesManager.sendChangeDeviceDescriptionRequest
import com.xabber.android.data.extension.devices.DevicesManager.sendRevokeDeviceRequest
import com.xabber.android.data.extension.devices.SessionVO
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.databinding.DeviceBottomSheetDialogBinding
import com.xabber.android.ui.helper.dipToPx
import com.xabber.android.ui.text.getDateTimeText
import com.xabber.android.ui.text.getHumanReadableEstimatedTime
import com.xabber.xmpp.smack.XMPPTCPConnection
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Stanza
import java.util.*

class DeviceInfoBottomSheetDialog : BottomSheetDialogFragment() {

    var onDismissListener: DialogInterface.OnDismissListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DeviceBottomSheetDialogBinding.inflate(inflater, container, false)

        initialSetup(
            accountJid = arguments?.getParcelable(ACCOUNT_JID_KEY) ?: throw IllegalArgumentException("DeviceInfoBottomSheetDialog needs a non null account jid!"),
            session = arguments?.getParcelable(SESSION_KEY) ?: throw IllegalArgumentException("DeviceInfoBottomSheetDialog needs a non null session!"),
            isCurrent = arguments?.getBoolean(IS_CURRENT) ?: false,
            binding = binding
        )

        return binding.root
    }

    private fun initialSetup(
        accountJid: AccountJid,
        session: SessionVO,
        isCurrent: Boolean,
        binding: DeviceBottomSheetDialogBinding
    ) {
        binding.devicesDescriptionTitle.text = session.description ?: session.client

        val statusValue = session.createSmartLastSeen(accountJid, PresenceManager)
        binding.devicesStatusValue.text = statusValue
        if (statusValue != "Online") {
            binding.devicesStatusLabel.text = context?.getString(R.string.device__info__status__label_last_seen)
        }

        binding.devicesDeviceValue.text = session.device
        binding.devicesDeviceIcon.setImageResource(getDeviceIcon(session))

        binding.devicesClientValue.text = session.client
        binding.devicesIpValue.text = session.ip

        binding.devicesExpireValue.text =
            session.expire.toLongOrNull()?.let { Date(it) }?.getDateTimeText() ?: session.expire

        binding.devicesTerminateRoot.setOnClickListener {
            showTerminateSessionDialog(accountJid, session.uid, session)
        }

        binding.devicesEditIcon.visibility = if (isCurrent) View.VISIBLE else View.GONE
        binding.devicesEditIcon.setOnClickListener {
            AccountManager.getAccount(accountJid)?.connection?.let {
                showChangeDescriptionDialog(session, it)
            }
        }
    }

    private fun showTerminateSessionDialog(accountJid: AccountJid, deviceUid: String, session: SessionVO) {
        AlertDialog.Builder(context)
            .setMessage(R.string.terminate_session_title)
            .setPositiveButton(R.string.button_terminate) { _, _ ->
                removeItemCallback(session)
                AccountManager.getAccount(accountJid)?.connection?.let {
                    sendRevokeDeviceRequest(it, deviceUid)
                }
                dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialogInterface, _ -> dialogInterface.cancel() }
            .create()
            .show()
    }

    @DrawableRes
    private fun getDeviceIcon(session: SessionVO): Int {
        return when {
            session.client.contains("Xabber for Web") -> R.drawable.ic_device_web
            session.device.contains("Android") -> R.drawable.ic_device_cellphone
            session.device.contains("iOS") -> R.drawable.ic_device_cellphone
            else -> R.drawable.ic_device_desktop
        }
    }

    private fun showChangeDescriptionDialog(session: SessionVO, connection: XMPPTCPConnection) {
        val descriptionEditText = EditText(context)
        descriptionEditText.maxLines = 1
        session.description?.takeIf { it.isNotEmpty() }?.let { descriptionEditText.setText(it) }
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = dipToPx(20f, requireContext())
        params.rightMargin = dipToPx(16f, requireContext())
        descriptionEditText.layoutParams = params
        container.addView(descriptionEditText)
        if (session.description != null) {
            descriptionEditText.setText(session.description)
        }
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.devices__dialog__device_description))
            .setView(container)
            .setPositiveButton(getString(R.string.devices__dialog__set_description)) { _: DialogInterface?, _: Int ->
                sendChangeDeviceDescriptionRequest(
                    connection,
                    session.uid,
                    descriptionEditText.text.toString(),
                    { packet: Stanza? ->
                        Application.getInstance().runOnUiThread {
                            if (packet is IQ && packet.type == IQ.Type.result) {
                                dismiss()
                            }
                        }
                    }
                ) {
                    Application.getInstance().runOnUiThread {
                        Toast.makeText(
                            context,
                            getString(R.string.devices__dialog__failed_to_set_description),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create()
            .show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismissListener?.onDismiss(dialog)
        super.onDismiss(dialog)
    }

    companion object {

        private lateinit var removeItemCallback: (SessionVO) -> (Void)

        const val TAG = "com.xabber.android.ui.fragment.DeviceInfoBottomSheetDialog"

        private const val IS_CURRENT = "com.xabber.android.ui.fragment.DeviceInfoBottomSheetDialog.IS_CURRENT"
        private const val SESSION_KEY = "com.xabber.android.ui.fragment.DeviceInfoBottomSheetDialog.SESSION_KEY"
        private const val ACCOUNT_JID_KEY = "com.xabber.android.ui.fragment.DeviceInfoBottomSheetDialog.ACCOUNT_JID_KEY"

        fun newInstance(accountJid: AccountJid, currentSession: SessionVO, isCurrent: Boolean, itemCallback: (SessionVO) -> (Void)) =
            DeviceInfoBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_CURRENT, isCurrent)
                    putParcelable(SESSION_KEY, currentSession)
                    putParcelable(ACCOUNT_JID_KEY, accountJid)
                    removeItemCallback = itemCallback
                }
            }

    }

}