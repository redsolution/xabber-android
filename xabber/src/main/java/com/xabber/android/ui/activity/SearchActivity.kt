package com.xabber.android.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextMenu
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.entity.BaseEntity
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.groups.GroupInviteManager.hasActiveIncomingInvites
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.message.chat.RegularChat
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.color.StatusBarPainter
import com.xabber.android.ui.fragment.chatListFragment.ChatListAdapter
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment
import com.xabber.android.ui.fragment.chatListFragment.ChatListItemListener
import com.xabber.android.ui.helper.ContextMenuHelper
import com.xabber.android.ui.widget.DividerItemDecoration
import com.xabber.android.ui.widget.SearchToolbar
import com.xabber.android.ui.widget.ShortcutBuilder
import net.gcardone.junidecode.Junidecode.unidecode
import java.util.*
import kotlin.collections.ArrayList


class SearchActivity : ManagedActivity(), ChatListItemListener {

    /* Variables for intents */
    private var action: String? = null
    private var forwardedIds: ArrayList<String>? = null
    private var sendText: String? = null

    private lateinit var toolbar: SearchToolbar
    private lateinit var recyclerView: RecyclerView

    companion object {

        /* Constants for in app Intents */
        private const val ACTION_FORWARD =
            "com.xabber.android.ui.activity.SearchActivity.ACTION_FORWARD"
        private const val ACTION_SEARCH =
            "com.xabber.android.ui.activity.SearchActivity.ACTION_SEARCH"

        /* Intent extras ids */
        private const val FORWARDED_IDS_EXTRA =
            "com.xabber.android.ui.activity.SearchActivity.FORWARDED_IDS_EXTRA"

        /* Constants for saving state bundle */
        private const val SAVED_ACTION =
            "com.xabber.android.ui.activity.SearchActivity.SAVED_ACTION"
        private const val SAVED_FORWARDED_IDS =
            "com.xabber.android.ui.activity.SearchActivity.SAVED_FORWARDED_IDS"
        private const val SAVED_SEND_TEXT =
            "com.xabber.android.ui.activity.SearchActivity.SAVED_SEND_TEXT"

        fun createSearchIntent(context: Context) =
            Intent(context, SearchActivity::class.java).also { it.action = ACTION_SEARCH }

        fun createForwardIntent(context: Context, forwardedIds: List<String>) =
            Intent(context, SearchActivity::class.java).also {
                it.action = ACTION_FORWARD
                it.putStringArrayListExtra(FORWARDED_IDS_EXTRA, ArrayList(forwardedIds))
            }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            action?.let { putString(SAVED_ACTION, it) }
            forwardedIds?.let { putStringArrayList(SAVED_FORWARDED_IDS, it) }
            sendText?.let { putString(SAVED_SEND_TEXT, it) }
        }
        super.onSaveInstanceState(outState)
    }

    private fun initActions(savedInstanceState: Bundle?, intent: Intent?) {
        if (savedInstanceState != null) {
            savedInstanceState.getString(SAVED_ACTION)?.let { action = it }
            savedInstanceState.getStringArrayList(SAVED_FORWARDED_IDS)?.let { forwardedIds = it }
            savedInstanceState.getString(SAVED_SEND_TEXT)?.let { sendText = it }
        } else {
            action = intent?.action
            forwardedIds = intent?.getStringArrayListExtra(FORWARDED_IDS_EXTRA)
            sendText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        }
    }

    private fun initAndSetupToolbar() {
        toolbar = findViewById(R.id.search_toolbar)

        /* Set toolbar background color via current main user and theme */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbar.color = ColorManager.getInstance().accountPainter.defaultRippleColor
        } else {
            TypedValue().let {
                this.theme.resolveAttribute(R.attr.bars_color, it, true)
                toolbar.color = it.data
            }
        }

        toolbar.onBackPressedListener = SearchToolbar.OnBackPressedListener { onBackPressed() }
        toolbar.onTextChangedListener =
            SearchToolbar.OnTextChangedListener { text -> updateContactsList(text) }

        when (action) {
            ACTION_SEARCH -> toolbar.setSearch()
            ACTION_FORWARD -> toolbar.title = getString(R.string.dialog_forward_message__header)
            else -> toolbar.title = getString(R.string.dialog_choose_recipient_header)
        }

    }

    private fun initAndSetupStatusBar() {
        /* Set status bar background color via current main user and theme */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            StatusBarPainter.instanceUpdateWithDefaultColor(this)
        } else {
            TypedValue().let {
                this.theme.resolveAttribute(R.attr.bars_color, it, true)
                StatusBarPainter.instanceUpdateWIthColor(this, it.data)
            }
        }
    }

    private fun initAndSetupContactsList() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity).apply {
                orientation = RecyclerView.VERTICAL
            }

            adapter = ChatListAdapter(
                buildChatsListWithFilter(null),
                this@SearchActivity,
                swipable = false,
            ).apply {
                if (action != ACTION_SEARCH) {
                    isSavedMessagesSpecialText = true
                }
            }

            itemAnimator = null
            addItemDecoration(
                DividerItemDecoration(context, RecyclerView.VERTICAL).apply {
                    setChatListOffsetMode(
                        if (SettingsManager.contactsShowAvatars()) {
                            ChatListFragment.ChatListAvatarState.SHOW_AVATARS
                        } else {
                            ChatListFragment.ChatListAvatarState.DO_NOT_SHOW_AVATARS
                        }
                    )
                }
            )
        }

        updateContactsList()
    }

    private fun showEmptyStatePlaceholder(visibility: Boolean = true) {
        recyclerView.visibility = if (visibility) View.GONE else View.VISIBLE

        findViewById<TextView>(R.id.search_empty_state_placeholder).visibility =
            if (visibility) View.VISIBLE else View.GONE
    }

    private fun updateContactsList(filter: String = "") {
        (recyclerView.adapter as? ChatListAdapter)?.apply {
            clear()
            buildChatsListWithFilter(filter).also {
                setItems(it)
                showEmptyStatePlaceholder(it.isEmpty())
            }
        }?.also {
            it.notifyDataSetChanged()
        }
    }

    private fun buildChatsListWithFilter(filterString: String?): MutableList<AbstractChat> {
        if (filterString != null && filterString.isNotEmpty()) {
            return ChatManager.getInstance().chatsOfEnabledAccounts
                .filter { it.lastMessage != null && it.lastTime != null }
                .filteredByString(filterString)
                .sortedBy { it.lastTime }
                .unionWith(
                    RosterManager.getInstance().allContactsForEnabledAccounts
                        .filteredByString(filterString)
                        .map { RegularChat(it.account, it.contactJid) }
                )
        } else {
            val savedMessagesChats = AccountManager.enabledAccounts.map {
                val contactJid = ContactJid.from(it.bareJid.toString())
                ChatManager.getInstance().getChat(it, contactJid)
                    ?: ChatManager.getInstance().createRegularChat(it, contactJid)
            }

            return ChatManager.getInstance().chatsOfEnabledAccounts.toMutableList()
                .filter {
                    it.lastTime != null && it.account.bareJid.toString() != it.contactJid.bareJid.toString()
                }
                .sortedWith { chat1, chat2 ->
                    when {
                        chat1.account.bareJid.toString() == chat1.contactJid.bareJid.toString()
                                && chat2.account.bareJid.toString() != chat2.contactJid.bareJid.toString()
                                || chat1.lastTime > chat2.lastTime -> -1
                        chat2.account.bareJid.toString() == chat2.contactJid.bareJid.toString()
                                && chat1.account.bareJid.toString() != chat1.contactJid.bareJid.toString()
                                || chat2.lastTime > chat1.lastTime -> 1
                        else -> 0
                    }
                }
                .toMutableList()
                .apply {
                    addAll(0, savedMessagesChats)
                }
        }
    }

    private fun <T : BaseEntity> Collection<T>.filteredByString(filterString: String): List<T> {
        val transliteratedFilterString = unidecode(filterString)
        return this.filter { chat ->
            val savedMessagesOrEmpty =
                if (chat.account.bareJid.toString() == chat.contactJid.bareJid.toString()) {
                    resources.getString(R.string.saved_messages__header)
                        .lowercase(Locale.getDefault())
                } else {
                    ""
                }

            val contactName = RosterManager.getInstance()
                .getBestContact(chat.account, chat.contactJid)
                .name
                .toLowerCase(Locale.getDefault())

            chat.contactJid.toString().contains(filterString)
                    || chat.contactJid.toString().contains(transliteratedFilterString)
                    || contactName.contains(filterString)
                    || contactName.contains(transliteratedFilterString)
                    || savedMessagesOrEmpty.contains(filterString)
        }
    }

    private infix fun List<AbstractChat>.unionWith(
        contactsEmptyChats: List<AbstractChat>
    ): MutableList<AbstractChat> {
        val result = this.toMutableList()
        for (abstractChat in contactsEmptyChats) {
            var isDuplicating = false
            for (abstractChat1 in this) {
                if (abstractChat.contactJid === abstractChat1.contactJid) {
                    isDuplicating = true
                    break
                }
            }
            if (!isDuplicating) {
                result.add(abstractChat)
            }
        }
        return result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_search)

        initActions(savedInstanceState, intent)
        initAndSetupToolbar()
        initAndSetupStatusBar()
        initAndSetupContactsList()

        super.onCreate(savedInstanceState)
    }

    override fun onChatAvatarClick(contact: AbstractChat) {
        when (action) {
            ACTION_SEARCH ->
                if (hasActiveIncomingInvites(contact.account, contact.contactJid)) {
                    onChatItemClick(contact)
                } else {
                    startActivity(
                        ContactViewerActivity.createIntent(
                            this, contact.account, contact.contactJid
                        )
                    )
                }

            Intent.ACTION_SEND ->
                if (intent.type?.contains("text/plain") != true && intent.extras != null) {
                    intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let { uri ->
                        startActivity(
                            ChatActivity.createSendUriIntent(
                                this, contact.account, contact.contactJid, uri
                            )
                        )
                    }
                } else {
                    startActivity(
                        ChatActivity.createSendIntent(
                            this, contact.account, contact.contactJid, sendText
                        )
                    )
                }

            Intent.ACTION_SEND_MULTIPLE ->
                if (intent.extras != null) {
                    intent.getParcelableArrayListExtra<Uri?>(Intent.EXTRA_STREAM)?.let { uri ->
                        startActivity(
                            ChatActivity.createSendUrisIntent(
                                this, contact.account, contact.contactJid, uri
                            )
                        )
                    }
                }

            Intent.ACTION_CREATE_SHORTCUT -> {
                ShortcutBuilder.createPinnedShortcut(
                    this,
                    RosterManager.getInstance().getAbstractContact(
                        contact.account, contact.contactJid
                    )
                )?.let { intent ->
                    setResult(RESULT_OK, intent)
                }
            }

            ACTION_FORWARD ->
                forwardedIds?.let {
                    startActivity(
                        ChatActivity.createForwardIntent(
                            this, contact.account, contact.contactJid, it
                        )
                    )
                    if (contact.account.bareJid.toString().contains(contact.contactJid.bareJid.toString())){
                        Application.getInstance().runOnUiThreadDelay(200, ::finish)
                    }
                }

            else -> startActivityForResult(
                ChatActivity.createSpecificChatIntent(this, contact.account, contact.contactJid),
                MainActivity.CODE_OPEN_CHAT
            )
        }
    }

    override fun onChatItemClick(contact: AbstractChat) {
        when (action) {
            ACTION_SEARCH ->
                startActivityForResult(
                    ChatActivity.createSendIntent(this, contact.account, contact.contactJid, null),
                    MainActivity.CODE_OPEN_CHAT
                )

            Intent.ACTION_SEND ->
                if (intent.type?.contains("text/plain") != true) {
                    intent.getParcelableExtra<Uri?>(Intent.EXTRA_STREAM)?.let { uri ->
                        startActivity(
                            ChatActivity.createSendUriIntent(
                                this, contact.account, contact.contactJid, uri
                            )
                        )
                    }
                } else {
                    startActivity(
                        ChatActivity.createSendIntent(
                            this, contact.account, contact.contactJid, sendText
                        )
                    )
                }

            Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra<Uri?>(Intent.EXTRA_STREAM)?.let { uris ->
                    startActivity(
                        ChatActivity.createSendUrisIntent(
                            this, contact.account, contact.contactJid, uris
                        )
                    )
                }

            Intent.ACTION_CREATE_SHORTCUT -> {
                ShortcutBuilder.createPinnedShortcut(
                    this,
                    RosterManager.getInstance()
                        .getAbstractContact(contact.account, contact.contactJid)
                )?.let { intent ->
                    setResult(RESULT_OK, intent)
                }
            }

            ACTION_FORWARD ->
                forwardedIds?.let {
                    startActivity(
                        ChatActivity.createForwardIntent(
                            this, contact.account, contact.contactJid, it
                        )
                    )
                    if (contact.account.bareJid.toString().contains(contact.contactJid.bareJid.toString())){
                        Application.getInstance().runOnUiThreadDelay(200, ::finish)
                    }
                }

            else -> startActivityForResult(
                ChatActivity.createSpecificChatIntent(this, contact.account, contact.contactJid),
                MainActivity.CODE_OPEN_CHAT
            )
        }
    }

    override fun onChatItemContextMenu(menu: ContextMenu, contact: AbstractChat) {
        if (action == ACTION_SEARCH) {
            ContextMenuHelper.createContactContextMenu(
                this,
                { updateContactsList(toolbar.searchText ?: "") },
                contact.account,
                contact.contactJid,
                menu
            )
        }
    }

    /* ignore */
    override fun onChatItemSwiped(abstractContact: AbstractChat) {}

    /* ignore */
    override fun onListBecomeEmpty() {}

}