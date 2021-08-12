/*
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.xabber.android.R
import com.xabber.android.data.ActivityManager
import com.xabber.android.data.Application
import com.xabber.android.data.NetworkException
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import com.xabber.android.data.extension.attention.AttentionManager
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.intent.BaseAccountIntentBuilder
import com.xabber.android.data.intent.EntityIntentBuilder
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.data.message.NotificationState.NotificationMode
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.data.roster.PresenceManager.requestSubscription
import com.xabber.android.data.roster.RosterContact
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.*
import com.xabber.android.ui.activity.SearchActivity.Companion.createForwardIntent
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import com.xabber.android.ui.dialog.*
import com.xabber.android.ui.fragment.ChatFragment
import com.xabber.android.ui.fragment.ChatFragment.ChatViewerFragmentListener
import com.xabber.android.ui.helper.NewContactTitleInflater
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.ui.helper.UpdateBackpressure
import com.xabber.android.ui.helper.UpdateBackpressure.UpdatableObject
import com.xabber.android.ui.preferences.CustomNotifySettings
import com.xabber.android.ui.widget.BottomMessagesPanel
import org.jivesoftware.smack.packet.Presence
import org.jxmpp.stringprep.XmppStringprepException
import java.util.*

/**
 * Chat activity.
 *
 *
 *
 * @author alexander.ivanov
 */
class ChatActivity : ManagedActivity(), OnContactChangedListener, OnMessageUpdatedListener,
    OnAccountChangedListener, OnChatStateListener, ChatViewerFragmentListener,
    OnBlockedListChangedListener, Toolbar.OnMenuItemClickListener, OnNewMessageListener,
    UpdatableObject, PopupMenu.OnMenuItemClickListener, OnGroupPresenceUpdatedListener {

    private lateinit var accountJid: AccountJid
    private lateinit var contactJid: ContactJid

    private val updateBackpressure: UpdateBackpressure = UpdateBackpressure(this)

    private var chatFragment: ChatFragment? = null

    private var extraText: String? = null
    private var needScrollToUnread = false

    private var attachmentUri: Uri? = null
    private var attachmentUris: ArrayList<Uri>? = null

    private var exitOnSend = false

    lateinit var toolbar: Toolbar
        private set

    private lateinit var contactTitleView: View

    //toolbar interaction panel variables
    private lateinit var interactionsRoot: View
    private lateinit var tvCount: TextView
    private lateinit var ivEdit: ImageView
    private lateinit var ivPin: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogManager.i(ChatActivity::class.java.simpleName, "onCreate $savedInstanceState")
        setContentView(R.layout.activity_chat)
        window.setBackgroundDrawable(null)

        contactTitleView = findViewById(R.id.contact_title)
        contactTitleView.setOnClickListener {
            if (accountJid.bareJid.toString() == contactJid.bareJid.toString()) {
                startActivity(AccountActivity.createIntent(this, accountJid))
            } else {
                startActivity(
                    ContactViewerActivity.createIntent(this@ChatActivity, accountJid, contactJid)
                )
            }
        }

        toolbar = findViewById(R.id.toolbar_default)

        findViewById<ImageView>(R.id.toolbar_arrow_back_iv)?.apply {
            setOnClickListener {
                attemptToClose()
            }
        }

        findViewById<ImageView>(R.id.toolbar_overflow_iv)?.apply {
            setOnClickListener {
                setUpOptionsMenu(it)
            }
        }

        interactionsRoot = findViewById(R.id.toolbar_chat_interactions_include)
        tvCount = findViewById(R.id.tvCount)
        findViewById<View>(R.id.ivClose).setOnClickListener {
            chatFragment?.onToolbarInteractionCloseClick()
        }

        ivPin = findViewById(R.id.ivPin)
        ivPin.setOnClickListener {
            chatFragment?.onToolbarInteractionPinClick()
        }

        findViewById<View>(R.id.ivDelete).setOnClickListener {
            chatFragment?.onToolbarInteractionDeleteClick()
        }

        findViewById<View>(R.id.ivCopy).setOnClickListener {
            chatFragment?.onToolbarInteractionCopyClick()
        }

        ivEdit = findViewById(R.id.ivEdit)
        ivEdit.setOnClickListener {
            chatFragment?.onToolbarInteractionsEditClick()
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        accountJid = BaseAccountIntentBuilder.getAccount(intent)
            ?: throw IllegalArgumentException("ChatActivity intent must contains an accountJid")
        contactJid = EntityIntentBuilder.getContactJid(intent)
            ?: throw IllegalArgumentException("ChatActivity intent must contains an contactJid")

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        } else {
            initChats()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LogManager.i(ChatActivity::class.java.simpleName, "onNewIntent")
        setIntent(intent)

        BaseAccountIntentBuilder.getAccount(intent)?.let { accountJid = it }
        EntityIntentBuilder.getContactJid(intent)?.let { contactJid = it }

        initChats()
    }

    override fun onResume() {
        super.onResume()
        LogManager.i(ChatActivity::class.java.simpleName, "onResume")
        updateToolbar()
        updateStatusBar()

        Application.getInstance().addUIListener(
            OnChatStateListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnContactChangedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnAccountChangedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnBlockedListChangedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnNewMessageListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnMessageUpdatedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnGroupPresenceUpdatedListener::class.java, this
        )

        when (intent.action) {
            ACTION_ATTENTION -> {
                AttentionManager.getInstance().removeAccountNotifications(accountJid, contactJid)
            }

            Intent.ACTION_SEND -> {
                if (intent.getParcelableExtra<Parcelable?>(Intent.EXTRA_STREAM) != null) {
                    val receivedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    intent.removeExtra(Intent.EXTRA_STREAM)
                    receivedUri?.let { handleShareFileUris(arrayListOf(it)) }
                } else {
                    extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (extraText != null) {
                        intent.removeExtra(Intent.EXTRA_TEXT)
                    }
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (uris != null) {
                    intent.removeExtra(Intent.EXTRA_STREAM)
                    handleShareFileUris(uris)
                }
            }
        }

        needScrollToUnread = intent.getBooleanExtra(EXTRA_NEED_SCROLL_TO_UNREAD, false)

        if (intent.getBooleanExtra(EXTRA_OTR_REQUEST, false)
            || intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)
        ) {
            handleOtrIntent(intent)
        }

        // forward
        if (ACTION_FORWARD == intent.action) {
            intent.getStringArrayListExtra(KEY_MESSAGES_ID)?.let {
                setForwardMessages(it)
            }
            intent.removeExtra(KEY_MESSAGES_ID)
        }
        insertExtraText()
    }

    private fun attemptToClose() {
        if (chatFragment?.tryToResetEditingText() != true) {
            close()
        }
    }

    fun showToolbarInteractionsPanel(
        isVisible: Boolean, isEditable: Boolean, isPinnable: Boolean, messagesCount: Int
    ) {
        if (isVisible) {
            interactionsRoot.visibility = View.VISIBLE
            contactTitleView.visibility = View.GONE

            if (isEditable) {
                ivEdit.visibility = View.VISIBLE
            } else {
                ivEdit.visibility = View.GONE
            }

            if (isPinnable) {
                ivPin.visibility = View.VISIBLE
            } else {
                ivPin.visibility = View.GONE
            }

            tvCount.text = messagesCount.toString()
        } else {
            interactionsRoot.visibility = View.GONE
            contactTitleView.visibility = View.VISIBLE
        }
    }

    private fun handleShareFileUris(uris: ArrayList<Uri>?) {
        if (uris == null || uris.size == 0) {
            Toast.makeText(this, R.string.could_not_get_path_to_file, Toast.LENGTH_SHORT).show()
            return
        }
        if (uris.size > 10) {
            Toast.makeText(this, R.string.too_many_files_at_once, Toast.LENGTH_SHORT).show()
            return
        }
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(
                this,
                PERMISSIONS_REQUEST_ATTACH_FILE
            )
        ) {
            if (HttpFileUploadManager.getInstance().isFileUploadSupported(accountJid)) {
                HttpFileUploadManager.getInstance().uploadFileViaUri(
                    accountJid, contactJid, uris, this
                )
            } else {
                showUploadNotSupportedDialog()
            }
        } else {
            attachmentUris = uris
        }
        if (accountJid.bareJid.toString() == contactJid.bareJid.toString()) {
            chatFragment?.sendMessage()
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_ATTACH_FILE) {
            when (grantResults[0]) {
                PackageManager.PERMISSION_GRANTED -> {
                    when (intent.action) {
                        Intent.ACTION_SEND -> {
                            attachmentUri?.let {
                                handleShareFileUris(arrayListOf(it))
                            }
                        }
                        Intent.ACTION_SEND_MULTIPLE -> handleShareFileUris(attachmentUris)
                    }
                }
                PackageManager.PERMISSION_DENIED -> {
                    Toast.makeText(this, R.string.no_permission_storage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleOtrIntent(intent: Intent) {
        val account = intent.getStringExtra(KEY_ACCOUNT)
        val user = intent.getStringExtra(KEY_USER)
        val question = intent.getStringExtra(KEY_QUESTION)
        if (account != null && user != null) {
            try {
                val accountJid = AccountJid.from(account)
                val contactJid = ContactJid.from(user)
                val chat = ChatManager.getInstance().getChat(accountJid, contactJid)
                if (chat is RegularChat) {
                    if (intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)) {
                        chat.intent = QuestionActivity.createCancelIntent(
                            Application.getInstance(), accountJid, contactJid
                        )
                    } else {
                        chat.intent = QuestionActivity.createIntent(
                            Application.getInstance(),
                            accountJid,
                            contactJid,
                            question != null,
                            true,
                            question
                        )
                    }
                }
            } catch (e: ContactJidCreateException) {
                LogManager.exception(javaClass.simpleName, e)
            } catch (e: XmppStringprepException) {
                LogManager.exception(javaClass.simpleName, e)
            }
        }
        getIntent().removeExtra(EXTRA_OTR_REQUEST)
        getIntent().removeExtra(EXTRA_OTR_PROGRESS)
    }

    private fun showUploadNotSupportedDialog() {
        AlertDialog.Builder(this)
            .setMessage(
                resources.getString(
                    R.string.error_file_upload_not_support,
                    accountJid.fullJid.domain.toString()
                )
            )
            .setTitle(getString(R.string.error_sending_file, ""))
            .setPositiveButton(R.string.ok) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .create()
            .show()
    }

    override fun onBackPressed() {
        attemptToClose()
    }

    private fun initChats() {
        val fragment: Fragment
        val oldFragment = supportFragmentManager.findFragmentByTag(CHAT_FRAGMENT_TAG)
        fragment = ChatFragment.newInstance(accountJid, contactJid)
        if (oldFragment != null) {
            val fragmentTransactionOld = supportFragmentManager.beginTransaction()
            fragmentTransactionOld.remove(oldFragment)
            fragmentTransactionOld.commit()
        }
        val fragmentTransactionNew = supportFragmentManager.beginTransaction()
        fragmentTransactionNew.add(R.id.chat_container, fragment, CHAT_FRAGMENT_TAG)
        fragmentTransactionNew.commit()
    }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        accountJid = savedInstanceState.getParcelable(SAVE_SELECTED_ACCOUNT)
            ?: throw IllegalArgumentException("Error while state restoring, saved instance bundle must contains an accountJid")
        contactJid = savedInstanceState.getParcelable(SAVE_SELECTED_USER)
            ?: throw IllegalArgumentException("Error while state restoring, saved instance bundle must contains an contactJid")

        exitOnSend = savedInstanceState.getBoolean(SAVE_EXIT_ON_SEND)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putParcelable(SAVE_SELECTED_ACCOUNT, accountJid)
            putParcelable(SAVE_SELECTED_USER, contactJid)
            putBoolean(SAVE_EXIT_ON_SEND, exitOnSend)
        }
    }

    override fun onGroupPresenceUpdated(
        accountJid: AccountJid, groupJid: ContactJid,
        presence: Presence
    ) {
        update()
    }

    override fun onPause() {
        super.onPause()
        updateBackpressure.removeRefreshRequests()

        Application.getInstance().removeUIListener(
            OnChatStateListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnContactChangedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnAccountChangedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnBlockedListChangedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnNewMessageListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnMessageUpdatedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnGroupPresenceUpdatedListener::class.java, this
        )

        if (exitOnSend) {
            ActivityManager.getInstance().cancelTask(this)
        }
    }

    override fun update() {
        updateToolbar()
        chatFragment?.updateContact()
        updateStatusBar()
    }

    override fun onAction() {
        Application.getInstance().runOnUiThread {
            updateBackpressure.refreshRequest()
        }
    }

    override fun onChatStateChanged(entities: Collection<RosterContact>) {
        if (entities.any { it.equals(accountJid, contactJid) }) {
            Application.getInstance().runOnUiThread {
                updateBackpressure.refreshRequest()
            }
        }
    }

    override fun onContactsChanged(entities: Collection<RosterContact>) {
        if (entities.any { it.equals(accountJid, contactJid) }) {
            Application.getInstance().runOnUiThread {
                updateBackpressure.refreshRequest()
            }
        }
    }

    override fun onAccountsChanged(accounts: Collection<AccountJid>) {
        if (accounts.contains(accountJid)) {
            Application.getInstance().runOnUiThread {
                updateBackpressure.refreshRequest()
            }
        }
    }

    private fun updateToolbar() {
        NewContactTitleInflater.updateTitle(
            contactTitleView,
            this,
            RosterManager.getInstance().getBestContact(accountJid, contactJid),
            ChatManager.getInstance().getChat(accountJid, contactJid)
                ?.notificationState
                ?.determineModeByGlobalSettings()
                ?: NotificationMode.byDefault
        )

        /* Update background color via current main user and theme; */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.setBackgroundColor(
                ColorManager.getInstance().accountPainter.getAccountRippleColor(
                    accountJid
                )
            )
        } else {
            val typedValue = TypedValue()
            this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            toolbar.setBackgroundColor(typedValue.data)
        }
    }

    private fun updateStatusBar() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            StatusBarPainter.instanceUpdateWithAccountName(this, accountJid)
        } else {
            val typedValue = TypedValue()
            this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            StatusBarPainter.instanceUpdateWIthColor(this, typedValue.data)
        }
    }

    private fun setForwardMessages(forwardsIds: ArrayList<String>) {
        if (forwardsIds.isEmpty() || chatFragment == null) {
            return
        }
        chatFragment?.showBottomMessagesPanel(
            forwardsIds,
            BottomMessagesPanel.Purposes.FORWARDING
        )
        if (accountJid.bareJid.toString() == contactJid.bareJid.toString()) {
            chatFragment?.sendMessage()
            finish()
        }
    }

    fun hideForwardPanel() {
        chatFragment?.hideBottomMessagePanel()
    }

    fun setUpVoiceMessagePresenter(tempPath: String?) {
        chatFragment?.setVoicePresenterData(tempPath)
    }

    fun finishVoiceRecordLayout() {
        chatFragment?.clearVoiceMessage()
        chatFragment?.finishVoiceRecordLayout()
    }

    private fun insertExtraText() {
        if (extraText == null || extraText == "") {
            return
        }
        chatFragment?.setInputText(extraText!!)
        extraText = null
        exitOnSend = true
        if (accountJid.bareJid.toString() == contactJid.bareJid.toString()) {
            chatFragment?.sendMessage()
            finish()
        }
    }

    override fun onMessageSent() {}

    override fun registerChatFragment(chatFragment: ChatFragment?) {
        this.chatFragment = chatFragment
    }

    override fun unregisterChatFragment() {
        chatFragment = null
    }

    private fun close() {
        chatFragment?.cleanUpVoice(true)
        update()
        finish()
        ActivityManager.getInstance().clearStack(false)
        startActivity(MainActivity.createClearStackIntent(this))
    }

    override fun onBlockedListChanged(account: AccountJid?) {
        // if chat of blocked contact is currently opened, it should be closed
        Application.getInstance().runOnUiThread {
            if (BlockingManager.getInstance().getCachedBlockedContacts(account)
                    .contains(contactJid)
            ) {
                close()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return onMenuItemClick(item)
    }

    private fun setUpOptionsMenu(view: View?) {
        ChatManager.getInstance().getChat(accountJid, contactJid)?.let { abstractChat ->
            PopupMenu(this, view).apply {
                if (accountJid.bareJid.toString() == contactJid.bareJid.toString()) {
                    menuInflater.inflate(R.menu.menu_chat_saved_messages, menu)
                } else {
                    menuInflater.inflate(R.menu.menu_chat_regular, menu)

                    // archive/unarchive chat
                    menu.findItem(R.id.action_archive_chat).isVisible = !abstractChat.isArchived
                    menu.findItem(R.id.action_unarchive_chat).isVisible = abstractChat.isArchived

                    // mute chat
                    menu.findItem(R.id.action_mute_chat).isVisible =
                        abstractChat.notifyAboutMessage()
                    menu.findItem(R.id.action_unmute_chat).isVisible =
                        !abstractChat.notifyAboutMessage()
                }
                setOnMenuItemClickListener { item: MenuItem ->
                    onMenuItemClick(item)
                }
            }.show()
        }
    }

    @SuppressLint("NonConstantResourceId")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        val abstractChat = ChatManager.getInstance().getChat(accountJid, contactJid)
        return when (item.itemId) {
            R.id.action_start_encryption -> {
                chatFragment?.showResourceChoiceAlert(accountJid, contactJid, false)
                true
            }
            R.id.action_restart_encryption -> {
                chatFragment?.showResourceChoiceAlert(accountJid, contactJid, true)
                true
            }
            R.id.action_stop_encryption -> {
                chatFragment?.stopEncryption(accountJid, contactJid)
                true
            }
            R.id.action_verify_with_fingerprint -> {
                startActivity(
                    FingerprintActivity.createIntent(this, accountJid, contactJid)
                )
                true
            }
            R.id.action_verify_with_question -> {
                startActivity(
                    QuestionActivity.createIntent(
                        this, accountJid, contactJid, true, false, null
                    )
                )
                true
            }
            R.id.action_verify_with_shared_secret -> {
                startActivity(
                    QuestionActivity.createIntent(
                        this, accountJid, contactJid, false, false, null
                    )
                )
                true
            }
            R.id.action_send_contact -> {
                sendContact()
                true
            }
            R.id.action_generate_qrcode -> {
                QRCodeActivity.createIntent(this, accountJid)
                    .apply {
                        putExtra(
                            QRCodeActivity.ACCOUNT_NAME_ARG,
                            RosterManager.getInstance()
                                .getRosterContact(accountJid, contactJid)?.name ?: ""
                        )
                        putExtra(QRCodeActivity.ACCOUNT_ADDRESS_ARG, contactJid.toString())
                    }.also { startActivity(it) }
                true
            }
            R.id.action_view_contact -> {
                chatFragment?.showContactInfo()
                true
            }
            R.id.action_configure_notifications -> {
                startActivity(
                    CustomNotifySettings.createIntent(this, accountJid, contactJid)
                )
                true
            }
            R.id.action_clear_history -> {
                chatFragment?.clearHistory(accountJid, contactJid)
                true
            }
            R.id.action_export_chat -> {
                chatFragment?.onExportChatClick()
                true
            }
            R.id.action_call_attention -> {
                chatFragment?.callAttention()
                true
            }
            R.id.action_block_contact -> {
                BlockContactDialog.newInstance(accountJid, contactJid).show(
                    supportFragmentManager, BlockContactDialog::class.java.name
                )
                true
            }
            R.id.action_request_subscription -> {
                try {
                    requestSubscription(accountJid, contactJid)
                } catch (e: NetworkException) {
                    Application.getInstance().onError(e)
                }
                true
            }
            R.id.action_archive_chat -> {
                abstractChat?.isArchived = true
                true
            }
            R.id.action_unarchive_chat -> {
                abstractChat?.isArchived = false
                true
            }
            R.id.action_mute_chat -> {
                SnoozeDialog.newInstance(abstractChat) {
                    updateToolbar()
                }.show(supportFragmentManager, SnoozeDialog.TAG)
                true
            }
            R.id.action_unmute_chat -> {
                abstractChat?.setNotificationStateOrDefault(
                    NotificationState(NotificationMode.enabled, 0),
                    true
                )
                updateToolbar()
                true
            }
            R.id.action_edit_contact -> {
                startActivity(
                    ContactEditActivity.createIntent(this, accountJid, contactJid)
                )
                true
            }
            R.id.action_remove_contact -> {
                ContactDeleteDialog.newInstance(accountJid, contactJid).show(
                    supportFragmentManager, ContactDeleteDialog::class.java.name
                )
                true
            }
            R.id.action_delete_chat -> {
                ChatDeleteDialog.newInstance(accountJid, contactJid).show(
                    supportFragmentManager, ChatDeleteDialog::class.java.name
                )
                false
            }
            else -> false
        }
    }

    private fun sendContact() {
        val text =
            RosterManager.getInstance().getRosterContact(accountJid, contactJid)
                ?.let { "${it.name}xmpp:$contactJid".trimIndent() }
                ?: "xmpp:$contactJid"

        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }.also {
            startActivity(
                Intent.createChooser(it, resources.getText(R.string.send_to))
            )
        }
    }

    fun forwardMessages(messagesIds: List<String>) {
        startActivity(
            createForwardIntent(this, messagesIds)
        )
    }

    fun showAttachDialog() {
        AttachDialog.newInstance(chatFragment).also {
            it.show(supportFragmentManager, "attach_fragment")
        }
    }

    fun needScrollToUnread(): Boolean {
        return if (needScrollToUnread) {
            needScrollToUnread = false
            true
        } else {
            false
        }
    }

    companion object {
        private const val CHAT_FRAGMENT_TAG = "CHAT_FRAGMENT_TAG"
        private const val ACTION_ATTENTION = "com.xabber.android.data.ATTENTION"
        private const val ACTION_SPECIFIC_CHAT = "com.xabber.android.data.ACTION_SPECIFIC_CHAT"
        private const val ACTION_FORWARD = "com.xabber.android.data.ACTION_FORWARD"
        const val EXTRA_NEED_SCROLL_TO_UNREAD =
            "com.xabber.android.data.EXTRA_NEED_SCROLL_TO_UNREAD"
        const val EXTRA_OTR_REQUEST = "com.xabber.android.data.EXTRA_OTR_REQUEST"
        const val EXTRA_OTR_PROGRESS = "com.xabber.android.data.EXTRA_OTR_PROGRESS"
        private const val PERMISSIONS_REQUEST_ATTACH_FILE = 24
        const val KEY_ACCOUNT = "KEY_ACCOUNT"
        const val KEY_USER = "KEY_USER"
        const val KEY_QUESTION = "KEY_QUESTION"
        const val KEY_SHOW_ARCHIVED = "KEY_SHOW_ARCHIVED"
        const val KEY_MESSAGES_ID = "KEY_MESSAGES_ID"
        private const val SAVE_SELECTED_ACCOUNT =
            "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_ACCOUNT"
        private const val SAVE_SELECTED_USER =
            "com.xabber.android.ui.activity.ChatActivity.SAVE_SELECTED_USER"
        private const val SAVE_EXIT_ON_SEND =
            "com.xabber.android.ui.activity.ChatActivity.SAVE_EXIT_ON_SEND"

        fun createSpecificChatIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?
        ): Intent = EntityIntentBuilder(context, ChatActivity::class.java)
            .setAccount(account)
            .setUser(user)
            .build()
            .apply {
                action = ACTION_SPECIFIC_CHAT
                ChatManager.getInstance().getChat(account, user).let { chat ->
                    putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived)
                }
            }

        fun createClearTopIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?
        ) = createSpecificChatIntent(context, account, user).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        fun createForwardIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?,
            messagesIds: ArrayList<String>
        ): Intent = EntityIntentBuilder(context, ChatActivity::class.java)
            .setAccount(account)
            .setUser(user).build()
            .apply {
                action = ACTION_FORWARD
                putStringArrayListExtra(KEY_MESSAGES_ID, messagesIds)
            }

        /**
         * Create intent to send message.
         *
         *
         * Contact list will not be shown on when chat will be closed.
         * @param text    if `null` then user will be able to send a number
         * of messages. Else only one message can be send.
         */
        fun createSendIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?,
            text: String?
        ) = createSpecificChatIntent(context, account, user).apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            ChatManager.getInstance().getChat(account, user).let { chat ->
                putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived)
            }
        }

        fun createSendUriIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?,
            uri: Uri?
        ) = createSpecificChatIntent(context, account, user).apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        fun createSendUrisIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?,
            uris: ArrayList<Uri?>?
        ) = createSpecificChatIntent(context, account, user).apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }

        fun createAttentionRequestIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?
        ) = createClearTopIntent(context, account, user).apply {
            action = ACTION_ATTENTION
        }

    }

}