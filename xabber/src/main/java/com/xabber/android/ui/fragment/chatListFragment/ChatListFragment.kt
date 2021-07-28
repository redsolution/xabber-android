package com.xabber.android.ui.fragment.chatListFragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.xabber.android.R
import com.xabber.android.data.Application
import com.xabber.android.data.SettingsManager
import com.xabber.android.data.account.AccountManager
import com.xabber.android.data.account.CommonState
import com.xabber.android.data.account.StatusMode
import com.xabber.android.data.connection.ConnectionState
import com.xabber.android.data.entity.AccountJid
import com.xabber.android.data.entity.ContactJid
import com.xabber.android.data.extension.avatar.AvatarManager
import com.xabber.android.data.extension.groups.GroupInviteManager.hasActiveIncomingInvites
import com.xabber.android.data.log.LogManager
import com.xabber.android.data.message.chat.AbstractChat
import com.xabber.android.data.message.chat.ChatManager
import com.xabber.android.data.notification.MessageNotificationManager
import com.xabber.android.data.roster.AbstractContact
import com.xabber.android.data.roster.RosterContact
import com.xabber.android.data.roster.RosterManager
import com.xabber.android.ui.*
import com.xabber.android.ui.activity.AccountActivity
import com.xabber.android.ui.activity.AddActivity
import com.xabber.android.ui.activity.ContactAddActivity.Companion.createIntent
import com.xabber.android.ui.activity.ContactViewerActivity
import com.xabber.android.ui.activity.MainActivity
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.helper.ContextMenuHelper
import com.xabber.android.ui.helper.ContextMenuHelper.ListPresenter
import com.xabber.android.ui.widget.DividerItemDecoration
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import java.util.*
import java.util.concurrent.TimeUnit

class ChatListFragment : Fragment(), ChatListItemListener, View.OnClickListener, OnChatStateListener,
    PopupMenu.OnMenuItemClickListener, ListPresenter, OnMessageUpdatedListener, OnStatusChangeListener,
    OnConnectionStateChangedListener, OnChatUpdatedListener {
    private var adapter: ChatListAdapter? = null
    private var items: MutableList<AbstractChat>? = null
    private var snackbar: Snackbar? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var chatListFragmentListener: ChatListFragmentListener? = null
    var currentChatsState = ChatListState.recent
        private set
    private var recyclerView: RecyclerView? = null
    private var markAllAsReadButton: TextView? = null
    private var markAllReadBackground: Drawable? = null
    private var maxItemsOnScreen = 0

    /* Placeholder variables */
    private var placeholderView: View? = null
    private var placeholderMessage: TextView? = null
    private var placeholderButton: Button? = null
    private var showPlaceholders = 0

    /* Toolbar variables */
    private var toolbarRelativeLayout: RelativeLayout? = null
    private var toolbarAppBarLayout: AppBarLayout? = null
    private var toolbarToolbarLayout: Toolbar? = null
    private var toolbarAccountColorIndicator: View? = null
    private var toolbarAccountColorIndicatorBack: View? = null
    private var toolbarTitleTv: TextView? = null
    private var toolbarAvatarIv: ImageView? = null
    private var toolbarStatusIv: ImageView? = null
    private val updateRequest = PublishSubject.create<Any?>()
    override fun onAttach(context: Context) {
        if (getContext() is ChatListFragmentListener) {
            chatListFragmentListener = context as ChatListFragmentListener
            chatListFragmentListener!!.onChatListStateChanged(currentChatsState)
        } else {
            LogManager.exception(
                ChatListFragment::class.java.simpleName, Exception(
                    "Context must implement " +
                            "ChatListFragmentListener"
                )
            )
        }
        super.onAttach(context)
    }

    override fun onDestroy() {
        if (chatListFragmentListener != null) {
            chatListFragmentListener = null
        }
        super.onDestroy()
    }

    override fun onStop() {
        Application.getInstance().removeUIListener(OnChatStateListener::class.java, this)
        Application.getInstance().removeUIListener(
            OnStatusChangeListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnConnectionStateChangedListener::class.java, this
        )
        Application.getInstance().removeUIListener(
            OnMessageUpdatedListener::class.java, this
        )
        Application.getInstance().removeUIListener(OnChatUpdatedListener::class.java, this)
        super.onStop()
    }

    override fun onPause() {
        super.onPause()
        MessageNotificationManager.getInstance().setShowBanners(true)
    }

    override fun onResume() {
        Application.getInstance().addUIListener(OnChatStateListener::class.java, this)
        Application.getInstance().addUIListener(OnStatusChangeListener::class.java, this)
        Application.getInstance().addUIListener(
            OnConnectionStateChangedListener::class.java, this
        )
        Application.getInstance().addUIListener(
            OnMessageUpdatedListener::class.java, this
        )
        Application.getInstance().addUIListener(OnChatUpdatedListener::class.java, this)
        MessageNotificationManager.getInstance().setShowBanners(false)
        var unreadCount = 0
        for (abstractChat in ChatManager.getInstance().chatsOfEnabledAccounts) if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived) {
            unreadCount += abstractChat.unreadMessageCount
        }
        if (unreadCount == 0) {
            currentChatsState = ChatListState.recent
            chatListFragmentListener!!.onChatListStateChanged(ChatListState.recent)
        }
        update()
        super.onResume()
    }

    override fun onStatusChanged(account: AccountJid?, user: ContactJid?, statusText: String?) {
        Application.getInstance().runOnUiThread { updateRequest.onNext(null) }
    }

    override fun onStatusChanged(
        account: AccountJid?,
        user: ContactJid?,
        statusMode: StatusMode?,
        statusText: String?
    ) {
        Application.getInstance().runOnUiThread { updateRequest.onNext(null) }
    }

    override fun onConnectionStateChanged(newConnectionState: ConnectionState) {
        Application.getInstance().runOnUiThread { updateRequest.onNext(null) }
    }

    override fun onAction() {
        Application.getInstance().runOnUiThread { updateRequest.onNext(null) }
    }

    fun onStateSelected(state: ChatListState) {
        currentChatsState = state
        chatListFragmentListener!!.onChatListStateChanged(state)
        toolbarAppBarLayout!!.setExpanded(true, false)
        update()
        closeSnackbar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun scrollToTop() {
        if (recyclerView != null && recyclerView!!.adapter!!.itemCount != 0) {
            recyclerView!!.scrollToPosition(0)
            toolbarAppBarLayout!!.setExpanded(true, false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        recyclerView = view.findViewById(R.id.chatlist_recyclerview)
        linearLayoutManager = LinearLayoutManager(activity)
        recyclerView.setLayoutManager(linearLayoutManager)
        coordinatorLayout = view.findViewById(R.id.chatlist_coordinator_layout)
        markAllAsReadButton = view.findViewById(R.id.mark_all_as_read_button)
        markAllReadBackground = view.resources.getDrawable(R.drawable.unread_button_background)
        if (Build.VERSION.SDK_INT >= 21) {
            markAllAsReadButton.setElevation(2f)
        }
        if (Build.VERSION.SDK_INT >= 16) {
            markAllAsReadButton.setBackground(markAllReadBackground)
        }
        placeholderView = view.findViewById(R.id.chatlist_placeholder_view)
        placeholderMessage = view.findViewById(R.id.chatlist_placeholder_message)
        placeholderButton = view.findViewById(R.id.chatlist_placeholder_button)
        items = ArrayList()
        adapter = ChatListAdapter(items, this, true)
        recyclerView.setAdapter(adapter)
        recyclerView.setItemAnimator(null)
        val divider = DividerItemDecoration(recyclerView.getContext(), linearLayoutManager!!.orientation)
        divider.setChatListOffsetMode(if (SettingsManager.contactsShowAvatars()) ChatListAvatarState.SHOW_AVATARS else ChatListAvatarState.DO_NOT_SHOW_AVATARS)
        recyclerView.addItemDecoration(divider)
        MessageNotificationManager.getInstance().removeAllMessageNotifications()
        chatListFragmentListener!!.onChatListStateChanged(currentChatsState)

        /* Toolbar variables initialization */toolbarRelativeLayout = view.findViewById(R.id.toolbar_chatlist)
        toolbarToolbarLayout = view.findViewById(R.id.chat_list_toolbar)
        toolbarAccountColorIndicator = view.findViewById(R.id.accountColorIndicator)
        toolbarAccountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack)
        val toolbarAddIv = view.findViewById<ImageView>(R.id.ivAdd)
        toolbarTitleTv = view.findViewById(R.id.tvTitle)
        toolbarAvatarIv = view.findViewById(R.id.ivAvatar)
        toolbarStatusIv = view.findViewById(R.id.ivStatus)
        toolbarAppBarLayout = view.findViewById(R.id.chatlist_toolbar_root)
        toolbarTitleTv.setText(Application.getInstance().applicationContext.getString(R.string.account_state_connecting))
        toolbarAddIv.setOnClickListener { v: View? -> startActivity(Intent(activity, AddActivity::class.java)) }
        toolbarAvatarIv.setOnClickListener(this)
        toolbarTitleTv.setOnClickListener(this)
        if (activity!!.javaClass.simpleName != MainActivity::class.java.simpleName) {
            toolbarAppBarLayout.setVisibility(View.GONE)
        }

        /* Find possible max recycler items*/
        val displayMetrics = context!!.resources.displayMetrics
        val dpHeight = Math.round(displayMetrics.heightPixels / displayMetrics.density)
        maxItemsOnScreen = Math.round(((dpHeight - 56 - 56) / 64).toFloat())
        showPlaceholders = 0
        return view
    }

    override fun updateContactList() {
        updateRequest.onNext(null)
    }

    /**
     * Update toolbarRelativeLayout via current state
     */
    private fun updateToolbar() {
        /* Update ChatState TextView display via current chat and connection state */
        if (AccountManager.getInstance().commonState == CommonState.online) {
            toolbarTitleTv!!.setText(R.string.application_title_full)
        } else {
            when (currentChatsState) {
                ChatListState.unread -> toolbarTitleTv!!.setText(R.string.unread_chats)
                ChatListState.archived -> toolbarTitleTv!!.setText(R.string.archived_chats)
                else -> toolbarTitleTv!!.setText(R.string.account_state_connecting)
            }
        }

        /* Update avatar and status ImageViews via current settings and main user */if (SettingsManager.contactsShowAvatars()) {
            toolbarAvatarIv!!.visibility = View.VISIBLE
            toolbarStatusIv!!.visibility = View.VISIBLE
            val mainAccountDrawable = AvatarManager.getInstance().mainAccountAvatar
            if (mainAccountDrawable != null) {
                toolbarAvatarIv!!.setImageDrawable(mainAccountDrawable)
            }
            if (AccountManager.getInstance().enabledAccounts.size > 0) {
                val accountJid = AccountManager.getInstance().firstAccount
                var mainAccountStatusMode = StatusMode.unavailable.statusLevel
                if (accountJid != null) {
                    mainAccountStatusMode = AccountManager.getInstance()
                        .getAccount(accountJid)
                        .getDisplayStatusMode()
                        .statusLevel
                }
                toolbarStatusIv!!.setImageLevel(mainAccountStatusMode)
            } else {
                toolbarStatusIv!!.setImageLevel(StatusMode.unavailable.ordinal)
            }
        } else {
            toolbarAvatarIv!!.visibility = View.GONE
            toolbarStatusIv!!.visibility = View.GONE
        }

        /* Update background color via current main user and theme; */if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbarRelativeLayout!!.setBackgroundColor(ColorManager.getInstance().accountPainter.defaultRippleColor)
        } else if (context != null) {
            val typedValue = TypedValue()
            val theme = context!!.theme
            theme.resolveAttribute(R.attr.bars_color, typedValue, true)
            toolbarRelativeLayout!!.setBackgroundColor(typedValue.data)
        }

        /* Update left color indicator via current main user */if (AccountManager.getInstance().enabledAccounts.size > 1) {
            toolbarAccountColorIndicator!!.setBackgroundColor(
                ColorManager.getInstance().accountPainter.defaultMainColor
            )
            toolbarAccountColorIndicatorBack!!.setBackgroundColor(
                ColorManager.getInstance().accountPainter.defaultIndicatorBackColor
            )
        } else {
            toolbarAccountColorIndicator!!.setBackgroundColor(Color.TRANSPARENT)
            toolbarAccountColorIndicatorBack!!.setBackgroundColor(Color.TRANSPARENT)
        }
        setupToolbarLayout()
    }

    /**
     * OnClickListener for Toolbar
     */
    override fun onClick(v: View) {
        when (v.id) {
            R.id.ivAvatar -> startActivity(
                AccountActivity.createIntent(activity, AccountManager.getInstance().firstAccount)
            )
            R.id.tvTitle -> showTitlePopup(toolbarTitleTv)
        }
    }

    /**
     * @return Return true when first element of chat list is on the top of the screen
     */
    val isOnTop: Boolean
        get() = linearLayoutManager!!.findFirstCompletelyVisibleItemPosition() == 0

    /**
     * @return Size of chat list
     */
    val listSize: Int
        get() = items!!.size

    /**
     * Show menu Chat state
     */
    private fun showTitlePopup(v: View?) {
        val popupMenu = PopupMenu(context!!, v!!)
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.inflate(R.menu.menu_chat_list)
        popupMenu.show()
    }

    /**
     * Handle toolbarRelativeLayout menus clicks
     */
    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_recent_chats -> {
                onStateSelected(ChatListState.recent)
                true
            }
            R.id.action_unread_chats -> {
                onStateSelected(ChatListState.unread)
                true
            }
            R.id.action_archived_chats -> {
                onStateSelected(ChatListState.archived)
                true
            }
            else -> false
        }
    }

    /**
     * Update chat items in adapter
     */
    private fun updateItems(newItems: List<AbstractChat>) {
        val tempIsOnTop = isOnTop
        if (newItems.size == 0 && showPlaceholders >= 3) {
            when (currentChatsState) {
                ChatListState.unread -> showPlaceholder(
                    Application.getInstance().applicationContext.getString(
                        R.string.placeholder_no_unread
                    ), null
                )
                ChatListState.archived -> showPlaceholder(
                    Application.getInstance().applicationContext.getString(R.string.placeholder_no_archived),
                    null
                )
                else -> {
                    showPlaceholder(
                        Application.getInstance().applicationContext.getString(R.string.application_state_no_contacts),
                        Application.getInstance().applicationContext.getString(R.string.application_action_no_contacts)
                    )
                    placeholderButton!!.setOnClickListener { view: View? ->
                        startActivity(
                            createIntent(
                                activity
                            )
                        )
                    }
                }
            }
        } else {
            hidePlaceholder()
        }

        /* Update items in RecyclerView */
        val diffResult = DiffUtil.calculateDiff(ChatItemDiffUtil(items!!, newItems, adapter!!), false)
        items!!.clear()
        items!!.addAll(newItems)
        adapter!!.addItems(newItems)
        diffResult.dispatchUpdatesTo(adapter!!)
        if (tempIsOnTop) scrollToTop()
    }

    override fun onChatStateChanged(entities: Collection<RosterContact?>) {
        Application.getInstance().runOnUiThread { update() }
    }

    /**
     * Setup Toolbar scroll behavior according to count of visible chat items
     */
    private fun setupToolbarLayout() {
        if (recyclerView != null) {
            setToolbarScrollEnabled(items!!.size > maxItemsOnScreen)
        }
    }

    /**
     * Enable or disable Toolbar scroll behavior
     */
    private fun setToolbarScrollEnabled(enabled: Boolean) {
        val toolbarLayoutParams = toolbarToolbarLayout!!.layoutParams as AppBarLayout.LayoutParams
        val appBarLayoutParams = toolbarAppBarLayout!!.layoutParams as CoordinatorLayout.LayoutParams
        if (enabled && toolbarLayoutParams.scrollFlags == 0) {
            appBarLayoutParams.behavior = AppBarLayout.Behavior()
            toolbarLayoutParams.scrollFlags =
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else if (!enabled && toolbarLayoutParams.scrollFlags != 0) {
            toolbarLayoutParams.scrollFlags = 0
            appBarLayoutParams.behavior = null
        }
        toolbarToolbarLayout!!.layoutParams = toolbarLayoutParams
        toolbarAppBarLayout!!.layoutParams = appBarLayoutParams
    }

    override fun onChatItemSwiped(abstractContact: AbstractChat) {
        val abstractChat = ChatManager.getInstance().getChat(abstractContact.account, abstractContact.contactJid)
        ChatManager.getInstance().getChat(
            abstractContact.account,
            abstractContact.contactJid
        )!!.isArchived = !abstractChat!!.isArchived
        showSnackbar(abstractContact, currentChatsState)
        updateRequest.onNext(null)
    }

    override fun onChatAvatarClick(item: AbstractChat) {
        if (hasActiveIncomingInvites(item.account, item.contactJid)) {
            onChatItemClick(item)
        } else {
            val intent: Intent
            try {
                intent = ContactViewerActivity.createIntent(activity, item.account, item.contactJid)
                activity!!.startActivity(intent)
            } catch (e: Exception) {
                LogManager.exception(ChatListFragment::class.java.toString(), e)
            }
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onChatItemContextMenu(menu: ContextMenu, contact: AbstractChat) {
        try {
            ContextMenuHelper.createContactContextMenu(
                activity,
                this,
                contact.account,
                contact.contactJid,
                menu
            )
        } catch (e: Exception) {
            LogManager.exception(ChatListFragment::class.java.toString(), e)
        }
    }

    override fun onListBecomeEmpty() {
        if (currentChatsState != ChatListState.recent) {
            currentChatsState = ChatListState.recent
        }
        updateRequest.onNext(null)
    }

    override fun onChatItemClick(item: AbstractChat) {
        try {
            chatListFragmentListener!!.onChatClick(
                RosterManager.getInstance()
                    .getAbstractContact(item.account, item.contactJid)
            )
        } catch (e: Exception) {
            LogManager.exception(ChatListFragment::class.java.toString(), e)
        }
    }

    private fun update() {
        val newList: MutableList<AbstractChat> = ArrayList()
        val allChats = ChatManager.getInstance().chatsOfEnabledAccounts
        when (currentChatsState) {
            ChatListState.recent -> for (abstractChat in allChats) if ((abstractChat.lastMessage != null
                        || hasActiveIncomingInvites(abstractChat.account, abstractChat.contactJid))
                && !abstractChat.isArchived
            ) {
                newList.add(abstractChat)
            }
            ChatListState.unread -> for (abstractChat in ChatManager.getInstance().chatsOfEnabledAccounts) if ((abstractChat.lastMessage != null
                        || hasActiveIncomingInvites(abstractChat.account, abstractChat.contactJid))
                && abstractChat.unreadMessageCount != 0
            ) {
                newList.add(abstractChat)
            }
            ChatListState.archived -> for (abstractChat in ChatManager.getInstance().chatsOfEnabledAccounts) if ((abstractChat.lastMessage != null
                        || hasActiveIncomingInvites(abstractChat.account, abstractChat.contactJid))
                && abstractChat.isArchived
            ) {
                newList.add(abstractChat)
            }
        }
        Collections.sort(newList) { o1: AbstractChat, o2: AbstractChat ->
            java.lang.Long.compare(
                o2.lastTime.time,
                o1.lastTime.time
            )
        }
        setupMarkAllTheReadButton(newList.size)

        /* Update another elements */updateToolbar()
        updateItems(newList)
        if (chatListFragmentListener != null) {
            chatListFragmentListener!!.onChatListUpdated()
        }
    }

    private fun setupMarkAllTheReadButton(listSize: Int) {
        if (currentChatsState == ChatListState.unread && listSize > 0 && context != null) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                markAllReadBackground!!.setColorFilter(
                    ColorManager.getInstance().accountPainter.defaultMainColor, PorterDuff.Mode.SRC_ATOP
                )
                markAllAsReadButton!!.setTextColor(context!!.resources.getColor(R.color.white))
            } else {
                markAllReadBackground!!.setColorFilter(
                    context!!.resources.getColor(R.color.grey_900), PorterDuff.Mode.SRC_ATOP
                )
                markAllAsReadButton!!.setTextColor(ColorManager.getInstance().accountPainter.defaultMainColor)
            }
            markAllAsReadButton!!.visibility = View.VISIBLE
            markAllAsReadButton!!.setOnClickListener { v: View? ->
                LogManager.d("ChatListFragment", "manually executing markAsReadAll")
                for (chat in ChatManager.getInstance().chatsOfEnabledAccounts) {
                    chat.markAsReadAll(true)
                    MessageNotificationManager.getInstance().removeAllMessageNotifications()
                }
                onStateSelected(ChatListState.recent)
                val toast = Toast.makeText(
                    activity, R.string.all_chats_were_market_as_read_toast,
                    Toast.LENGTH_SHORT
                )
                toast.setGravity(
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0,
                    (resources.getDimension(R.dimen.bottom_navigation_height) * 1.2f).toInt()
                )
                toast.show()
            }
        } else {
            markAllAsReadButton!!.visibility = View.GONE
        }
    }

    private fun showPlaceholder(message: String, buttonMessage: String?) {
        placeholderMessage!!.text = message
        if (buttonMessage != null) {
            placeholderButton!!.visibility = View.VISIBLE
            placeholderButton!!.text = buttonMessage
        }
        placeholderView!!.visibility = View.VISIBLE
    }

    private fun hidePlaceholder() {
        recyclerView!!.visibility = View.VISIBLE
        placeholderView!!.visibility = View.GONE
        placeholderButton!!.visibility = View.GONE
    }

    private fun showSnackbar(deletedItem: AbstractChat, previousState: ChatListState) {
        if (snackbar != null) {
            snackbar!!.dismiss()
        }
        val abstractChat = ChatManager.getInstance().getChat(deletedItem.account, deletedItem.contactJid)
        val archived = abstractChat!!.isArchived
        snackbar = Snackbar.make(
            coordinatorLayout!!,
            if (!archived) R.string.chat_was_unarchived else R.string.chat_was_archived,
            Snackbar.LENGTH_LONG
        )
        snackbar!!.setAction(
            R.string.undo
        ) { view: View? ->
            abstractChat.isArchived = !archived
            onStateSelected(previousState)
            updateRequest.onNext(null)
        }
        snackbar!!.setActionTextColor(Color.YELLOW)
        snackbar!!.show()
    }

    private fun closeSnackbar() {
        if (snackbar != null) {
            snackbar!!.dismiss()
        }
    }

    enum class ChatListState {
        recent, unread, archived
    }

    interface ChatListFragmentListener {
        fun onChatClick(contact: AbstractContact?)
        fun onChatListStateChanged(chatListState: ChatListState?)
        fun onChatListUpdated()
    }

    enum class ChatListAvatarState {
        NOT_SPECIFIED, SHOW_AVATARS, DO_NOT_SHOW_AVATARS
    }

    companion object {
        fun newInstance(account: AccountJid?): ChatListFragment {
            val fragment = ChatListFragment()
            val args = Bundle()
            if (account != null) {
                args.putSerializable("account_jid", account)
            }
            fragment.arguments = args
            return fragment
        }
    }

    init {
        updateRequest
            .debounce(1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { action: Any? -> update() }
    }
}