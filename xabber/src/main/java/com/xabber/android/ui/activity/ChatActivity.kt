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
import android.app.Activity
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
import android.view.inputmethod.InputMethodManager
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
import com.xabber.android.data.roster.PresenceManager.requestSubscription
import com.xabber.android.ui.activity.SearchActivity.Companion.createForwardIntent
import com.xabber.android.ui.activity.ManagedActivity
import com.xabber.android.ui.OnContactChangedListener
import com.xabber.android.ui.OnMessageUpdatedListener
import com.xabber.android.ui.OnAccountChangedListener
import com.xabber.android.ui.OnChatStateListener
import com.xabber.android.ui.fragment.ChatFragment.ChatViewerFragmentListener
import com.xabber.android.ui.OnBlockedListChangedListener
import com.xabber.android.ui.OnNewMessageListener
import com.xabber.android.ui.helper.UpdateBackpressure.UpdatableObject
import com.xabber.android.ui.dialog.SnoozeDialog.OnSnoozeListener
import com.xabber.android.ui.OnGroupPresenceUpdatedListener
import com.xabber.android.ui.helper.UpdateBackpressure
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.ui.fragment.ChatFragment
import com.xabber.android.ui.activity.ChatActivity
import com.xabber.android.ui.activity.AccountActivity
import com.xabber.android.ui.activity.ContactViewerActivity
import com.xabber.android.data.extension.attention.AttentionManager
import com.xabber.android.ui.helper.PermissionsRequester
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.ui.activity.QuestionActivity
import com.xabber.android.data.entity.ContactJid.ContactJidCreateException
import org.jxmpp.stringprep.XmppStringprepException
import com.xabber.android.data.roster.RosterContact
import com.xabber.android.data.entity.BaseEntity
import com.xabber.android.ui.helper.NewContactTitleInflater
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import com.xabber.android.ui.widget.BottomMessagesPanel
import com.xabber.android.ui.activity.MainActivity
import com.xabber.android.data.extension.blocking.BlockingManager
import com.xabber.android.ui.activity.FingerprintActivity
import com.xabber.android.ui.preferences.CustomNotifySettings
import com.xabber.android.ui.dialog.BlockContactDialog
import com.xabber.android.data.roster.PresenceManager
import com.xabber.android.data.NetworkException
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager
import com.xabber.android.data.message.NotificationState
import com.xabber.android.ui.activity.ContactEditActivity
import com.xabber.android.ui.dialog.ContactDeleteDialog
import com.xabber.android.ui.dialog.ChatDeleteDialog
import com.xabber.android.data.message.NotificationState.NotificationMode
import com.xabber.android.ui.activity.SearchActivity
import com.xabber.android.ui.dialog.AttachDialog
import com.xabber.android.ui.dialog.SnoozeDialog
import com.xabber.android.ui.activity.QRCodeActivity
import com.xabber.android.data.intent.EntityIntentBuilder
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.ChatManager
import org.jivesoftware.smack.packet.Presence
import java.util.ArrayList

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
    UpdatableObject, OnSnoozeListener, PopupMenu.OnMenuItemClickListener,
    OnGroupPresenceUpdatedListener {
    private var updateBackpressure: UpdateBackpressure? = null
    private var extraText: String? = null
    private var forwardsIds: ArrayList<String>? = null
    private var attachmentUri: Uri? = null
    private var attachmentUris: ArrayList<Uri>? = null
    private var account: AccountJid? = null
    private var user: ContactJid? = null
    private var exitOnSend = false
    private var chatFragment: ChatFragment? = null
    var toolbar: Toolbar? = null
        private set
    private var contactTitleView: View? = null
    private var toolbarOverflowIv: ImageView? = null

    //toolbar interaction panel variables
    private var interactionsRoot: View? = null
    private var tvCount: TextView? = null
    private var ivEdit: ImageView? = null
    private var ivPin: ImageView? = null
    var showArchived = false
    private var needScrollToUnread = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogManager.i(ChatActivity::class.java.simpleName, "onCreate $savedInstanceState")
        setContentView(R.layout.activity_chat)
        window.setBackgroundDrawable(null)
        updateBackpressure = UpdateBackpressure(this)
        contactTitleView = findViewById(R.id.contact_title)
        contactTitleView.setOnClickListener(View.OnClickListener { v: View? ->
            if (account!!.bareJid.toString() == user!!.bareJid.toString()) {
                startActivity(AccountActivity.createIntent(this, account))
            } else {
                startActivity(ContactViewerActivity.createIntent(this@ChatActivity, account, user))
            }
        })
        toolbar = findViewById(R.id.toolbar_default)
        val toolbarBackIv = findViewById<ImageView>(R.id.toolbar_arrow_back_iv)
        toolbarOverflowIv = findViewById(R.id.toolbar_overflow_iv)
        toolbarBackIv.setOnClickListener { v: View? -> attemptToClose() }
        toolbarOverflowIv.setOnClickListener(View.OnClickListener { v: View? ->
            setUpOptionsMenu(
                toolbarOverflowIv
            )
        })
        interactionsRoot = findViewById(R.id.toolbar_chat_interactions_include)
        tvCount = findViewById(R.id.tvCount)
        findViewById<View>(R.id.ivClose).setOnClickListener { v: View? ->
            if (chatFragment != null) {
                chatFragment!!.onToolbarInteractionCloseClick()
            }
        }
        ivPin = findViewById(R.id.ivPin)
        ivPin.setOnClickListener(View.OnClickListener { v: View? ->
            if (chatFragment != null) {
                chatFragment!!.onToolbarInteractionPinClick()
            }
        })
        findViewById<View>(R.id.ivDelete).setOnClickListener { v: View? ->
            if (chatFragment != null) {
                chatFragment!!.onToolbarInteractionDeleteClick()
            }
        }
        findViewById<View>(R.id.ivCopy).setOnClickListener { v: View? ->
            if (chatFragment != null) {
                chatFragment!!.onToolbarInteractionCopyClick()
            }
        }
        ivEdit = findViewById(R.id.ivEdit)
        ivEdit.setOnClickListener(View.OnClickListener { v: View? ->
            if (chatFragment != null) {
                chatFragment!!.onToolbarInteractionsEditClick()
            }
        })
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        val intent = intent
        account = getAccount(intent)
        user = getUser(intent)
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
        initialChatFromIntent
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
        showArchived = intent.getBooleanExtra(KEY_SHOW_ARCHIVED, false)
        val intent = intent
        if (hasAttention(intent)) {
            AttentionManager.getInstance().removeAccountNotifications(account, user)
        }
        if (Intent.ACTION_SEND == intent.action && intent.getParcelableExtra<Parcelable?>(Intent.EXTRA_STREAM) != null) {
            val receivedUri = getIntent().getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            intent.removeExtra(Intent.EXTRA_STREAM)
            handleShareFileUri(receivedUri)
        } else if (Intent.ACTION_SEND == intent.action) {
            extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (extraText != null) {
                intent.removeExtra(Intent.EXTRA_TEXT)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == intent.action) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            if (uris != null) {
                intent.removeExtra(Intent.EXTRA_STREAM)
                handleShareFileUris(uris)
            }
        }
        needScrollToUnread = intent.getBooleanExtra(EXTRA_NEED_SCROLL_TO_UNREAD, false)
        if (intent.getBooleanExtra(EXTRA_OTR_REQUEST, false) ||
            intent.getBooleanExtra(EXTRA_OTR_PROGRESS, false)
        ) {
            handleOtrIntent(intent)
        }

        // forward
        if (ACTION_FORWARD == intent.action) {
            forwardsIds = intent.getStringArrayListExtra(KEY_MESSAGES_ID)
            intent.removeExtra(KEY_MESSAGES_ID)
        }
        insertExtraText()
        setForwardMessages()
    }

    private fun attemptToClose() {
        if (!chatFragment!!.tryToResetEditingText()) {
            close()
        }
    }

    fun showToolbarInteractionsPanel(
        isVisible: Boolean, isEditable: Boolean, isPinnable: Boolean,
        messagesCount: Int
    ) {
        if (isVisible) {
            interactionsRoot!!.visibility = View.VISIBLE
            contactTitleView!!.visibility = View.GONE
            if (isEditable) {
                ivEdit!!.visibility = View.VISIBLE
            } else {
                ivEdit!!.visibility = View.GONE
            }
            if (isPinnable) {
                ivPin!!.visibility = View.VISIBLE
            } else {
                ivPin!!.visibility = View.GONE
            }
            tvCount!!.text = messagesCount.toString()
        } else {
            interactionsRoot!!.visibility = View.GONE
            contactTitleView!!.visibility = View.VISIBLE
        }
    }

    fun handleShareFileUri(fileUri: Uri?) {
        if (PermissionsRequester.requestFileReadPermissionIfNeeded(
                this,
                PERMISSIONS_REQUEST_ATTACH_FILE
            )
        ) {
            if (HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
                val uris: MutableList<Uri?> = ArrayList()
                uris.add(fileUri)
                HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, this)
            } else {
                showUploadNotSupportedDialog()
            }
        } else {
            attachmentUri = fileUri
        }
        if (account!!.bareJid.toString() == user!!.bareJid.toString()) {
            chatFragment!!.sendMessage()
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_ATTACH_FILE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (intent.action != null) {
                when (intent.action) {
                    Intent.ACTION_SEND -> handleShareFileUri(attachmentUri)
                    Intent.ACTION_SEND_MULTIPLE -> handleShareFileUris(attachmentUris)
                }
            }
        } else if (requestCode == PERMISSIONS_REQUEST_ATTACH_FILE
            && grantResults[0] == PackageManager.PERMISSION_DENIED
        ) {
            Toast.makeText(this, R.string.no_permission_storage, Toast.LENGTH_LONG).show()
        }
    }

    fun handleShareFileUris(uris: ArrayList<Uri>?) {
        if (uris!!.size == 0) {
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
            if (HttpFileUploadManager.getInstance().isFileUploadSupported(account)) {
                HttpFileUploadManager.getInstance().uploadFileViaUri(account, user, uris, this)
            } else {
                showUploadNotSupportedDialog()
            }
        } else {
            attachmentUris = uris
        }
        if (account!!.bareJid.toString() == user!!.bareJid.toString()) {
            chatFragment!!.sendMessage()
            finish()
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
        val serverName = account!!.fullJid.domain.toString()
        val builder = AlertDialog.Builder(this)
        builder.setMessage(
            this.resources.getString(
                R.string.error_file_upload_not_support,
                serverName
            )
        )
            .setTitle(getString(R.string.error_sending_file, ""))
            .setPositiveButton(R.string.ok) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onBackPressed() {
        attemptToClose()
    }

    private fun initChats() {
        val fragment: Fragment
        val oldFragment = supportFragmentManager.findFragmentByTag(CHAT_FRAGMENT_TAG)
        fragment = ChatFragment.newInstance(account, user)
        if (oldFragment != null) {
            val fragmentTransactionOld = supportFragmentManager.beginTransaction()
            fragmentTransactionOld.remove(oldFragment)
            fragmentTransactionOld.commit()
        }
        val fragmentTransactionNew = supportFragmentManager.beginTransaction()
        fragmentTransactionNew.add(R.id.chat_container, fragment, CHAT_FRAGMENT_TAG)
        fragmentTransactionNew.commit()
    }

    private val initialChatFromIntent: Unit
        private get() {
            val intent = intent
            val newAccount = getAccount(intent)
            val newUser = getUser(intent)
            if (newAccount != null) {
                account = newAccount
            }
            if (newUser != null) {
                user = newUser
            }
            initChats()
            LogManager.i(ChatActivity::class.java.simpleName, "getInitialChatFromIntent " + user)
        }

    private fun restoreInstanceState(savedInstanceState: Bundle) {
        account = savedInstanceState.getParcelable(SAVE_SELECTED_ACCOUNT)
        user = savedInstanceState.getParcelable(SAVE_SELECTED_USER)
        exitOnSend = savedInstanceState.getBoolean(SAVE_EXIT_ON_SEND)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SAVE_SELECTED_ACCOUNT, account)
        outState.putParcelable(SAVE_SELECTED_USER, user)
        outState.putBoolean(SAVE_EXIT_ON_SEND, exitOnSend)
    }

    override fun onGroupPresenceUpdated(
        accountJid: AccountJid, groupJid: ContactJid,
        presence: Presence
    ) {
        update()
    }

    override fun onPause() {
        super.onPause()
        updateBackpressure!!.removeRefreshRequests()
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

    override fun onStop() {
        super.onStop()
    }

    override fun update() {
        updateToolbar()
        updateChat()
        updateStatusBar()
    }

    override fun onAction() {
        Application.getInstance().runOnUiThread { updateBackpressure!!.refreshRequest() }
    }

    override fun onChatStateChanged(entities: Collection<RosterContact>) {
        for (contact in entities) {
            if (contact.contactJid.bareJid.equals(user!!.bareJid)) {
                Application.getInstance().runOnUiThread { updateToolbar() }
                return
            }
        }
    }

    override fun onContactsChanged(entities: Collection<RosterContact>) {
        for (entity in entities) {
            if (entity.equals(account, user)) {
                Application.getInstance().runOnUiThread { updateBackpressure!!.refreshRequest() }
                break
            }
        }
    }

    override fun onAccountsChanged(accounts: Collection<AccountJid?>?) {
        if (accounts != null && accounts.contains(account)) {
            Application.getInstance().runOnUiThread { updateBackpressure!!.refreshRequest() }
        }
    }

    private fun updateToolbar() {
        NewContactTitleInflater.updateTitle(
            contactTitleView,
            this,
            RosterManager.getInstance().getBestContact(account, user),
            notifMode
        )

        /* Update background color via current main user and theme; */if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar!!.setBackgroundColor(
                ColorManager.getInstance().accountPainter.getAccountRippleColor(
                    account
                )
            )
        } else {
            val typedValue = TypedValue()
            this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            toolbar!!.setBackgroundColor(typedValue.data)
        }
    }

    private fun updateStatusBar() {
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            StatusBarPainter.instanceUpdateWithAccountName(this, account)
        } else {
            val typedValue = TypedValue()
            this.theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            StatusBarPainter.instanceUpdateWIthColor(this, typedValue.data)
        }
    }

    private fun updateChat() {
        if (chatFragment != null) chatFragment!!.updateContact()
    }

    private fun setForwardMessages() {
        if (forwardsIds == null || forwardsIds!!.isEmpty() || chatFragment == null) {
            return
        }
        chatFragment!!.setBottomPanelMessagesIds(
            forwardsIds,
            BottomMessagesPanel.Purposes.FORWARDING
        )
        forwardsIds = null
        if (account!!.bareJid.toString() == user!!.bareJid.toString()) {
            chatFragment!!.sendMessage()
            finish()
        }
    }

    fun hideForwardPanel() {
        if (chatFragment == null) {
            return
        }
        chatFragment!!.hideBottomMessagePanel()
    }

    fun setUpVoiceMessagePresenter(tempPath: String?) {
        if (chatFragment == null) {
            return
        }
        chatFragment!!.setVoicePresenterData(tempPath)
    }

    fun finishVoiceRecordLayout() {
        if (chatFragment == null) {
            return
        }
        chatFragment!!.clearVoiceMessage()
        chatFragment!!.finishVoiceRecordLayout()
    }

    private fun insertExtraText() {
        if (extraText == null || extraText == "") {
            return
        }
        if (chatFragment != null) {
            chatFragment!!.setInputText(extraText)
            extraText = null
            exitOnSend = true
            if (account!!.bareJid.toString() == user!!.bareJid.toString()) {
                chatFragment!!.sendMessage()
                finish()
            }
        }
    }

    override fun onMessageSent() {}
    override fun registerChatFragment(chatFragment: ChatFragment) {
        this.chatFragment = chatFragment
    }

    override fun unregisterChatFragment() {
        chatFragment = null
    }

    private fun close() {
        if (chatFragment != null) {
            chatFragment!!.cleanUpVoice(true)
        }
        update()
        finish()
        ActivityManager.getInstance().clearStack(false)
        startActivity(MainActivity.createClearStackIntent(this))
    }

    override fun onBlockedListChanged(account: AccountJid?) {
        // if chat of blocked contact is currently opened, it should be closed
        Application.getInstance().runOnUiThread {
            if (BlockingManager.getInstance().getCachedBlockedContacts(account).contains(user)) {
                close()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return onMenuItemClick(item)
    }

    private fun setUpOptionsMenu(view: View?) {
        val abstractChat = ChatManager.getInstance().getChat(account, user)
        if (abstractChat != null) {
            val popupMenu = PopupMenu(this, view)
            if (account!!.bareJid.toString() == user!!.bareJid.toString()) {
                popupMenu.menuInflater.inflate(R.menu.menu_chat_saved_messages, popupMenu.menu)
            } else {
                popupMenu.menuInflater.inflate(R.menu.menu_chat_regular, popupMenu.menu)

                // archive/unarchive chat
                popupMenu.menu.findItem(R.id.action_archive_chat).isVisible =
                    !abstractChat.isArchived
                popupMenu.menu.findItem(R.id.action_unarchive_chat).isVisible =
                    abstractChat.isArchived

                // mute chat
                popupMenu.menu.findItem(R.id.action_mute_chat).isVisible =
                    abstractChat.notifyAboutMessage()
                popupMenu.menu.findItem(R.id.action_unmute_chat).isVisible =
                    !abstractChat.notifyAboutMessage()
            }
            popupMenu.setOnMenuItemClickListener { item: MenuItem -> onMenuItemClick(item) }
            popupMenu.show()
        }
    }

    @SuppressLint("NonConstantResourceId")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        val abstractChat = ChatManager.getInstance().getChat(account, user)
        return when (item.itemId) {
            R.id.action_start_encryption -> {
                if (chatFragment != null) chatFragment!!.showResourceChoiceAlert(
                    account,
                    user,
                    false
                )
                true
            }
            R.id.action_restart_encryption -> {
                if (chatFragment != null) chatFragment!!.showResourceChoiceAlert(
                    account,
                    user,
                    true
                )
                true
            }
            R.id.action_stop_encryption -> {
                if (chatFragment != null) chatFragment!!.stopEncryption(account, user)
                true
            }
            R.id.action_verify_with_fingerprint -> {
                startActivity(FingerprintActivity.createIntent(this, account, user))
                true
            }
            R.id.action_verify_with_question -> {
                startActivity(QuestionActivity.createIntent(this, account, user, true, false, null))
                true
            }
            R.id.action_verify_with_shared_secret -> {
                startActivity(
                    QuestionActivity.createIntent(
                        this,
                        account,
                        user,
                        false,
                        false,
                        null
                    )
                )
                true
            }
            R.id.action_send_contact -> {
                sendContact()
                true
            }
            R.id.action_generate_qrcode -> {
                generateQR()
                true
            }
            R.id.action_view_contact -> {
                if (chatFragment != null) chatFragment!!.showContactInfo()
                true
            }
            R.id.action_configure_notifications -> {
                startActivity(CustomNotifySettings.createIntent(this, account, user))
                true
            }
            R.id.action_clear_history -> {
                if (chatFragment != null) chatFragment!!.clearHistory(account, user)
                true
            }
            R.id.action_export_chat -> {
                if (chatFragment != null) chatFragment!!.onExportChatClick()
                true
            }
            R.id.action_call_attention -> {
                if (chatFragment != null) chatFragment!!.callAttention()
                true
            }
            R.id.action_block_contact -> {
                BlockContactDialog.newInstance(account, user)
                    .show(supportFragmentManager, BlockContactDialog::class.java.name)
                true
            }
            R.id.action_request_subscription -> {
                try {
                    requestSubscription(account!!, user!!)
                } catch (e: NetworkException) {
                    Application.getInstance().onError(e)
                }
                true
            }
            R.id.action_archive_chat -> {
                if (abstractChat != null) abstractChat.isArchived = true
                true
            }
            R.id.action_unarchive_chat -> {
                if (abstractChat != null) abstractChat.isArchived = false
                true
            }
            R.id.action_mute_chat -> {
                showSnoozeDialog(abstractChat)
                true
            }
            R.id.action_unmute_chat -> {
                abstractChat?.setNotificationStateOrDefault(
                    NotificationState(NotificationMode.enabled, 0),
                    true
                )
                onSnoozed()
                true
            }
            R.id.action_edit_contact -> {
                startActivity(ContactEditActivity.createIntent(this, account, user))
                true
            }
            R.id.action_remove_contact -> {
                ContactDeleteDialog.newInstance(account, user)
                    .show(supportFragmentManager, ContactDeleteDialog::class.java.name)
                true
            }
            R.id.action_delete_chat -> {
                ChatDeleteDialog.newInstance(account, user)
                    .show(supportFragmentManager, ChatDeleteDialog::class.java.name)
                false
            }
            else -> false
        }
    }

    private val notifMode: NotificationMode
        private get() {
            val chat = ChatManager.getInstance().getChat(account, user)
            return if (chat != null) {
                chat.notificationState.determineModeByGlobalSettings()
            } else {
                NotificationMode.byDefault
            }
        }

    private fun sendContact() {
        val rosterContact = RosterManager.getInstance().getRosterContact(account, user)
        val text = if (rosterContact != null) """
     ${rosterContact.name}
     xmpp:${user.toString()}
     """.trimIndent() else "xmpp:" + user.toString()
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        sendIntent.type = "text/plain"
        startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.send_to)))
    }

    fun forwardMessages(messagesIds: ArrayList<String?>?) {
        startActivity(createForwardIntent(this, messagesIds))
    }

    fun showAttachDialog() {
        if (chatFragment != null) {
            val dialog = AttachDialog.newInstance(chatFragment)
            dialog.show(supportFragmentManager, "attach_fragment")
        }
    }

    fun showSnoozeDialog(chat: AbstractChat?) {
        val dialog = SnoozeDialog.newInstance(chat, this)
        dialog.show(supportFragmentManager, "snooze_fragment")
    }

    override fun onSnoozed() {
        updateToolbar()
    }

    fun needScrollToUnread(): Boolean {
        return if (needScrollToUnread) {
            needScrollToUnread = false
            true
        } else {
            false
        }
    }

    private fun generateQR() {
        val rosterContact = RosterManager.getInstance().getRosterContact(account, user)
        val intent = QRCodeActivity.createIntent(this@ChatActivity, account)
        val textName = if (rosterContact != null) rosterContact.name else ""
        intent.putExtra("account_name", textName)
        val textAddress = user.toString()
        intent.putExtra("account_address", textAddress)
        intent.putExtra("caller", "ChatActivity")
        startActivity(intent)
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

        fun hideKeyboard(activity: Activity) {
            // Check if no view has focus:
            val view = activity.currentFocus
            if (view != null) {
                val inputManager =
                    activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(
                    view.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
        }

        private fun getAccount(intent: Intent): AccountJid? {
            val value = EntityIntentBuilder.getAccount(intent)
            if (value != null) {
                return value
            }
            // Backward compatibility.
            val stringExtra =
                intent.getStringExtra("com.xabber.android.data.account") ?: return null
            return try {
                AccountJid.from(stringExtra)
            } catch (e: XmppStringprepException) {
                LogManager.exception(ChatActivity::class.java.simpleName, e)
                null
            }
        }

        private fun getUser(intent: Intent): ContactJid? {
            val value = EntityIntentBuilder.getUser(intent)
            if (value != null) {
                return value
            }
            // Backward compatibility.
            val stringExtra = intent.getStringExtra("com.xabber.android.data.user") ?: return null
            return try {
                ContactJid.from(stringExtra)
            } catch (e: ContactJidCreateException) {
                LogManager.exception(ChatActivity::class.java.simpleName, e)
                null
            }
        }

        private fun hasAttention(intent: Intent): Boolean {
            return ACTION_ATTENTION == intent.action
        }

        fun createSpecificChatIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?
        ): Intent {
            val intent = EntityIntentBuilder(context, ChatActivity::class.java)
                .setAccount(account)
                .setUser(user)
                .build()
            intent.action = ACTION_SPECIFIC_CHAT
            val chat = ChatManager.getInstance().getChat(account, user)
            intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived)
            return intent
        }

        fun createClearTopIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?
        ): Intent {
            val intent = createSpecificChatIntent(context, account, user)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            return intent
        }

        fun createForwardIntent(
            context: Context?, account: AccountJid?, user: ContactJid?,
            messagesIds: ArrayList<String?>?
        ): Intent {
            val intent = EntityIntentBuilder(context, ChatActivity::class.java)
                .setAccount(account)
                .setUser(user).build()
            intent.action = ACTION_FORWARD
            intent.putStringArrayListExtra(KEY_MESSAGES_ID, messagesIds)
            return intent
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
        ): Intent {
            val intent = createSpecificChatIntent(context, account, user)
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, text)
            val chat = ChatManager.getInstance().getChat(account, user)
            intent.putExtra(KEY_SHOW_ARCHIVED, chat != null && chat.isArchived)
            return intent
        }

        fun createSendUriIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?,
            uri: Uri?
        ): Intent {
            val intent = createSpecificChatIntent(context, account, user)
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            return intent
        }

        fun createSendUrisIntent(
            context: Context?, account: AccountJid?, user: ContactJid?,
            uris: ArrayList<Uri?>?
        ): Intent {
            val intent = createSpecificChatIntent(context, account, user)
            intent.action = Intent.ACTION_SEND_MULTIPLE
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            return intent
        }

        fun createAttentionRequestIntent(
            context: Context?,
            account: AccountJid?,
            user: ContactJid?
        ): Intent {
            val intent = createClearTopIntent(context, account, user)
            intent.action = ACTION_ATTENTION
            return intent
        }
    }
}