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
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
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
import com.xabber.android.ui.activity.*
import com.xabber.android.ui.color.ColorManager
import com.xabber.android.ui.helper.ContextMenuHelper
import com.xabber.android.ui.helper.ContextMenuHelper.ListPresenter
import com.xabber.android.ui.widget.DividerItemDecoration
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.PublishSubject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class ChatListFragment : Fragment(), ChatListItemListener, View.OnClickListener,
    OnChatStateListener, ListPresenter, OnMessageUpdatedListener, OnStatusChangeListener,
    OnConnectionStateChangedListener, OnChatUpdatedListener {

    private var snackbar: Snackbar? = null
    private var chatListFragmentListener: ChatListFragmentListener? = null
    private val updateRequest = PublishSubject.create<Any?>()
    var currentChatsState = ChatListState.RECENT
        private set

    /* Chats list */
    private var items: MutableList<AbstractChat> = ArrayList()
    private val linearLayoutManager: LinearLayoutManager = LinearLayoutManager(activity)
    private val adapter: ChatListAdapter = ChatListAdapter(items, this, true)
    private lateinit var recyclerView: RecyclerView

    /* Mark all as read button */
    private lateinit var markAllAsReadButton: TextView
    private lateinit var markAllReadBackground: Drawable
    private var maxItemsOnScreen = 0

    /* Placeholder */
    private lateinit var placeholderView: View
    private lateinit var placeholderMessage: TextView
    private lateinit var placeholderButton: Button

    /* Toolbar */
    private lateinit var toolbarRelativeLayout: RelativeLayout
    private lateinit var toolbarAppBarLayout: AppBarLayout
    private lateinit var toolbarToolbarLayout: Toolbar
    private lateinit var toolbarAccountColorIndicator: View
    private lateinit var toolbarAccountColorIndicatorBack: View
    private lateinit var toolbarTitleTv: TextView
    private lateinit var toolbarAvatarIv: ImageView
    private lateinit var toolbarStatusIv: ImageView
    private lateinit var toolbarAddIv: ImageView

    override fun onAttach(context: Context) {
        if (getContext() is ChatListFragmentListener) {
            chatListFragmentListener = context as ChatListFragmentListener
            chatListFragmentListener?.onChatListStateChanged(currentChatsState)
        } else {
            LogManager.exception(
                this::class.java.simpleName,
                Exception("Context must implement ChatListFragmentListener")
            )
        }
        super.onAttach(context)
    }

    override fun onDestroy() {
        chatListFragmentListener = null
        super.onDestroy()
    }

    override fun onStop() {
        Application.getInstance().removeUIListener(OnChatStateListener::class.java, this)
        Application.getInstance().removeUIListener(OnStatusChangeListener::class.java, this)
        Application.getInstance()
            .removeUIListener(OnConnectionStateChangedListener::class.java, this)
        Application.getInstance().removeUIListener(OnMessageUpdatedListener::class.java, this)
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
        Application.getInstance().addUIListener(OnConnectionStateChangedListener::class.java, this)
        Application.getInstance().addUIListener(OnMessageUpdatedListener::class.java, this)
        Application.getInstance().addUIListener(OnChatUpdatedListener::class.java, this)

        MessageNotificationManager.getInstance().setShowBanners(false)

        val unreadCount =
            ChatManager.getInstance().chatsOfEnabledAccounts
                .filter { it.notifyAboutMessage() && !it.isArchived }
                .sumOf { it.unreadMessageCount }

        if (unreadCount == 0) {
            currentChatsState = ChatListState.RECENT
            chatListFragmentListener?.onChatListStateChanged(ChatListState.RECENT)
        }

        update()
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)

        recyclerView = view.findViewById(R.id.chatlist_recyclerview)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        recyclerView.addItemDecoration(
            DividerItemDecoration(recyclerView.context, linearLayoutManager.orientation)
                .apply {
                    setChatListOffsetMode(
                        if (SettingsManager.contactsShowAvatars()) {
                            ChatListAvatarState.SHOW_AVATARS
                        } else {
                            ChatListAvatarState.DO_NOT_SHOW_AVATARS
                        }
                    )
                }
        )

        markAllReadBackground =
            ResourcesCompat.getDrawable(resources, R.drawable.unread_button_background, null)!!
        markAllAsReadButton = view.findViewById(R.id.mark_all_as_read_button)
        markAllAsReadButton.apply {
            if (Build.VERSION.SDK_INT >= 21) {
                markAllAsReadButton.elevation = 2f
            }
            if (Build.VERSION.SDK_INT >= 16) {
                markAllAsReadButton.background = markAllReadBackground
            }
        }

        placeholderView = view.findViewById(R.id.chatlist_placeholder_view)
        placeholderMessage = view.findViewById(R.id.chatlist_placeholder_message)
        placeholderButton = view.findViewById(R.id.chatlist_placeholder_button)

        MessageNotificationManager.getInstance().removeAllMessageNotifications()

        chatListFragmentListener?.onChatListStateChanged(currentChatsState)

        /* Toolbar variables initialization */
        toolbarRelativeLayout = view.findViewById(R.id.toolbar_chatlist)
        toolbarToolbarLayout = view.findViewById(R.id.chat_list_toolbar)
        toolbarAccountColorIndicator = view.findViewById(R.id.accountColorIndicator)
        toolbarAccountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack)

        toolbarTitleTv = view.findViewById(R.id.tvTitle)
        toolbarTitleTv.apply {
            text = context?.getString(R.string.application_title_full)
            setOnClickListener(this@ChatListFragment)
        }

        toolbarAvatarIv = view.findViewById(R.id.ivAvatar)
        toolbarAvatarIv.setOnClickListener(this)

        toolbarStatusIv = view.findViewById(R.id.ivStatus)
        toolbarAppBarLayout = view.findViewById(R.id.chatlist_toolbar_root)
        if (activity?.javaClass?.simpleName != MainActivity::class.java.simpleName) {
            toolbarAppBarLayout.visibility = View.GONE
        }

        toolbarAddIv = view.findViewById(R.id.ivAdd)

        /* Find possible max recycler items*/
        context?.resources?.displayMetrics?.let { displayMetrics ->
            val dpHeight = (displayMetrics.heightPixels / displayMetrics.density).roundToInt()
            maxItemsOnScreen = ((dpHeight - 56 - 56) / 64).toFloat().roundToInt()
        }

        return view
    }

    override fun onStatusChanged(account: AccountJid?, user: ContactJid?, statusText: String?) {
        activity?.runOnUiThread { updateRequest.onNext(null) }
    }

    override fun onStatusChanged(
        account: AccountJid?, user: ContactJid?, statusMode: StatusMode?, statusText: String?
    ) {
        activity?.runOnUiThread { updateRequest.onNext(null) }
    }

    override fun onConnectionStateChanged(newConnectionState: ConnectionState) {
        activity?.runOnUiThread { updateRequest.onNext(null) }
    }

    override fun onAction() {
        activity?.runOnUiThread { updateRequest.onNext(null) }
    }

    fun onStateSelected(state: ChatListState) {
        currentChatsState = state
        chatListFragmentListener?.onChatListStateChanged(state)
        toolbarAppBarLayout.setExpanded(true, false)
        update()

        snackbar?.dismiss()
    }

    fun scrollToTop() {
        if (recyclerView.adapter?.itemCount != 0) {
            recyclerView.scrollToPosition(0)
            toolbarAppBarLayout.setExpanded(true, false)
        }
    }

    override fun updateContactList() {
        updateRequest.onNext(null)
    }

    /**
     * Update toolbarRelativeLayout via current state
     */
    private fun updateToolbar() {
        /* Update ChatState TextView display via current chat and connection state */
        toolbarTitleTv.setText(
            when {
                AccountManager.getInstance().commonState == CommonState.connecting -> {
                    R.string.account_state_connecting
                }
                AccountManager.getInstance().commonState == CommonState.waiting -> {
                    R.string.waiting_for_network
                }
                currentChatsState == ChatListState.UNREAD -> R.string.unread_chats
                currentChatsState == ChatListState.ARCHIVED -> R.string.archived_chats
                else -> R.string.application_title_full
            }
        )

        /* Update toolbar add iv behavior */
        when {
            !AccountManager.getInstance().hasAccounts() -> {
                toolbarAddIv.setOnClickListener {
                    startActivity(AccountAddActivity.createIntent(context))
                }
            }
            AccountManager.getInstance().enabledAccounts.isEmpty() -> {
                Toast.makeText(
                    context,
                    getString(R.string.application_state_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                toolbarAddIv.setOnClickListener {
                    startActivity(
                        Intent(activity, AddActivity::class.java)
                    )
                }
            }
        }

        /* Update avatar and status ImageViews via current settings and main user */
        if (SettingsManager.contactsShowAvatars()) {
            toolbarAvatarIv.visibility = View.VISIBLE
            toolbarStatusIv.visibility = View.VISIBLE

            AvatarManager.getInstance().mainAccountAvatar?.let {
                toolbarAvatarIv.setImageDrawable(it)
            }

            if (AccountManager.getInstance().enabledAccounts.isNotEmpty()) {
                toolbarStatusIv.setImageLevel(
                    AccountManager.getInstance().firstAccount?.let {
                        AccountManager.getInstance()
                            .getAccount(it)?.displayStatusMode?.statusLevel
                    } ?: StatusMode.unavailable.statusLevel
                )
                toolbarTitleTv.visibility = View.VISIBLE
            } else {
                context?.let {
                    toolbarAvatarIv.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            it.resources,
                            R.drawable.xabber_logo_80dp,
                            null
                        )
                    )
                }
                toolbarStatusIv.visibility = View.GONE
            }
        } else {
            toolbarAvatarIv.visibility = View.GONE
            toolbarStatusIv.visibility = View.GONE
        }

        /* Update background color via current main user and theme; */
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbarRelativeLayout.setBackgroundColor(ColorManager.getInstance().accountPainter.defaultRippleColor)
        } else if (context != null) {
            val typedValue = TypedValue()
            context?.theme?.resolveAttribute(R.attr.bars_color, typedValue, true)
            toolbarRelativeLayout.setBackgroundColor(typedValue.data)
        }

        /* Update left color indicator via current main user */
        if (AccountManager.getInstance().enabledAccounts.size > 1) {
            toolbarAccountColorIndicator.setBackgroundColor(
                ColorManager.getInstance().accountPainter.defaultMainColor
            )
            toolbarAccountColorIndicatorBack.setBackgroundColor(
                ColorManager.getInstance().accountPainter.defaultIndicatorBackColor
            )
        } else {
            toolbarAccountColorIndicator.setBackgroundColor(Color.TRANSPARENT)
            toolbarAccountColorIndicatorBack.setBackgroundColor(Color.TRANSPARENT)
        }

        val enabled = items.size > maxItemsOnScreen

        val toolbarLayoutParams = toolbarToolbarLayout.layoutParams as AppBarLayout.LayoutParams
        val appBarLayoutParams =
            toolbarAppBarLayout.layoutParams as CoordinatorLayout.LayoutParams

        if (enabled && toolbarLayoutParams.scrollFlags == 0) {
            appBarLayoutParams.behavior = AppBarLayout.Behavior()
            toolbarLayoutParams.scrollFlags =
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else if (!enabled && toolbarLayoutParams.scrollFlags != 0) {
            toolbarLayoutParams.scrollFlags = 0
            appBarLayoutParams.behavior = null
        }

        toolbarToolbarLayout.layoutParams = toolbarLayoutParams
        toolbarAppBarLayout.layoutParams = appBarLayoutParams
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
        get() = linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0

    /**
     * @return Size of chat list
     */
    val listSize: Int
        get() = items.size

    /**
     * Show menu Chat state
     */
    private fun showTitlePopup(v: View) =
        PopupMenu(context, v).apply {
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_recent_chats -> {
                        onStateSelected(ChatListState.RECENT)
                        true
                    }
                    R.id.action_unread_chats -> {
                        onStateSelected(ChatListState.UNREAD)
                        true
                    }
                    R.id.action_archived_chats -> {
                        onStateSelected(ChatListState.ARCHIVED)
                        true
                    }
                    else -> false
                }
            }
        }.also {
            it.inflate(R.menu.menu_chat_list)
            it.show()
        }

    /**
     * Update chat items in adapter
     */
    private fun updateItems(newItems: List<AbstractChat>) {
        val tempIsOnTop = isOnTop

        updatePlaceholderWithParams(newItems.isEmpty())

        /* Update items in RecyclerView */
        val diffResult =
            DiffUtil.calculateDiff(ChatItemDiffUtil(items, newItems, adapter), false)
        items.clear()
        items.addAll(newItems)
        adapter.addItems(newItems.toMutableList())
        diffResult.dispatchUpdatesTo(adapter)
        if (tempIsOnTop) {
            scrollToTop()
        }
    }

    override fun onChatStateChanged(entities: Collection<RosterContact>) {
        activity?.runOnUiThread { update() }
    }

    override fun onChatItemSwiped(abstractContact: AbstractChat) {
        val abstractChat =
            ChatManager.getInstance()
                .getChat(abstractContact.account, abstractContact.contactJid)
        ChatManager.getInstance()
            .getChat(abstractContact.account, abstractContact.contactJid)
            ?.isArchived = !abstractChat!!.isArchived
        showSnackbar(abstractContact, currentChatsState)
        updateRequest.onNext(null)
    }

    override fun onChatAvatarClick(contact: AbstractChat) {
        if (hasActiveIncomingInvites(contact.account, contact.contactJid)) {
            onChatItemClick(contact)
        } else {
            val intent: Intent
            try {
                intent = ContactViewerActivity.createIntent(
                    activity,
                    contact.account,
                    contact.contactJid
                )
                activity?.startActivity(intent)
            } catch (e: Exception) {
                LogManager.exception(ChatListFragment::class.java.toString(), e)
            }
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
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
        if (currentChatsState != ChatListState.RECENT) {
            currentChatsState = ChatListState.RECENT
        }
        updateRequest.onNext(null)
    }

    override fun onChatItemClick(contact: AbstractChat) {
        try {
            chatListFragmentListener?.onChatClick(
                RosterManager.getInstance()
                    .getAbstractContact(contact.account, contact.contactJid)
            )
        } catch (e: Exception) {
            LogManager.exception(ChatListFragment::class.java.toString(), e)
        }
    }

    private fun update() {
        val newList: MutableList<AbstractChat> = ArrayList()
        val allChats = ChatManager.getInstance().chatsOfEnabledAccounts
        when (currentChatsState) {
            ChatListState.RECENT -> for (abstractChat in allChats) if ((abstractChat.lastMessage != null
                        || hasActiveIncomingInvites(
                    abstractChat.account,
                    abstractChat.contactJid
                ))
                && !abstractChat.isArchived
            ) {
                newList.add(abstractChat)
            }
            ChatListState.UNREAD ->
                for (abstractChat in ChatManager.getInstance().chatsOfEnabledAccounts) {
                    if ((abstractChat.lastMessage != null
                                || hasActiveIncomingInvites(
                            abstractChat.account,
                            abstractChat.contactJid
                        ))
                        && abstractChat.unreadMessageCount != 0
                    ) {
                        newList.add(abstractChat)
                    }
                }
            ChatListState.ARCHIVED -> for (abstractChat in ChatManager.getInstance().chatsOfEnabledAccounts) if ((abstractChat.lastMessage != null
                        || hasActiveIncomingInvites(
                    abstractChat.account,
                    abstractChat.contactJid
                ))
                && abstractChat.isArchived
            ) {
                newList.add(abstractChat)
            }
        }
        newList.sortWith { o1: AbstractChat, o2: AbstractChat -> o2.lastTime.time.compareTo(o1.lastTime.time) }

        setupMarkAllTheReadButton(newList.size)

        /* Update another elements */
        updateToolbar()
        updateItems(newList)
        chatListFragmentListener?.onChatListUpdated()
    }

    private fun setupMarkAllTheReadButton(listSize: Int) {
        if (currentChatsState == ChatListState.UNREAD && listSize > 0 && context != null) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                markAllReadBackground.setColorFilter(
                    ColorManager.getInstance().accountPainter.defaultMainColor,
                    PorterDuff.Mode.SRC_ATOP
                )
                markAllAsReadButton.setTextColor(resources.getColor(R.color.white))
            } else {
                markAllReadBackground.setColorFilter(
                    resources.getColor(R.color.grey_900), PorterDuff.Mode.SRC_ATOP
                )
                markAllAsReadButton.setTextColor(ColorManager.getInstance().accountPainter.defaultMainColor)
            }
            markAllAsReadButton.visibility = View.VISIBLE
            markAllAsReadButton.setOnClickListener {
                LogManager.d("ChatListFragment", "manually executing markAsReadAll")
                for (chat in ChatManager.getInstance().chatsOfEnabledAccounts) {
                    chat.markAsReadAll(true)
                    MessageNotificationManager.getInstance().removeAllMessageNotifications()
                }
                onStateSelected(ChatListState.RECENT)
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
            markAllAsReadButton.visibility = View.GONE
        }
    }

    private fun updatePlaceholderWithParams(isChatsListEmpty: Boolean) {

        fun showPlaceholder(
            message: String,
            buttonMessage: String? = null,
            buttonListener: View.OnClickListener? = null
        ) {
            placeholderMessage.text = message

            if (buttonMessage != null) {
                placeholderButton.visibility = View.VISIBLE
                placeholderButton.text = buttonMessage
            } else {
                placeholderButton.visibility = View.GONE
            }

            buttonListener?.let { placeholderButton.setOnClickListener(it) }

            placeholderView.visibility = View.VISIBLE
        }

        fun hidePlaceholder() {
            recyclerView.visibility = View.VISIBLE
            placeholderView.visibility = View.GONE
            placeholderButton.visibility = View.GONE
        }

        val hasNoActiveContacts =
            RosterManager.getInstance().allContactsForEnabledAccounts.isEmpty()

        if (context == null) return

        when {
            !AccountManager.getInstance().hasAccounts() -> {
                showPlaceholder(
                    getString(R.string.application_state_empty),
                    getString(R.string.application_action_empty)
                ) {
                    startActivity(AccountAddActivity.createIntent(context))
                }
            }

            AccountManager.getInstance().enabledAccounts.isEmpty() -> {
                showPlaceholder(
                    getString(R.string.application_state_disabled),
                    getString(R.string.application_action_disabled)
                ) { chatListFragmentListener?.onManageAccountsClick() }
            }

            isChatsListEmpty && hasNoActiveContacts -> {
                showPlaceholder(
                    getString(R.string.application_state_no_contacts),
                    getString(R.string.application_action_no_contacts)
                ) { startActivity(ContactAddActivity.createIntent(context)) }
            }

            isChatsListEmpty ->
                when (currentChatsState) {
                    ChatListState.UNREAD -> {
                        showPlaceholder(getString(R.string.placeholder_no_unread))
                    }

                    ChatListState.ARCHIVED -> {
                        showPlaceholder(getString(R.string.placeholder_no_archived))
                    }

                    else -> {
                        showPlaceholder(
                            getString(R.string.placeholder_no_chats),
                            getString(R.string.application_action_lets_go)
                        ) { chatListFragmentListener?.onContactsClick() }
                    }
                }

            else -> hidePlaceholder()
        }

    }

    private fun showSnackbar(deletedItem: AbstractChat, previousState: ChatListState) {
        snackbar?.dismiss()

        val abstractChat =
            ChatManager.getInstance().getChat(deletedItem.account, deletedItem.contactJid)
                ?: return
        val archived = abstractChat.isArchived

        snackbar = view?.let {
            Snackbar.make(
                it,
                if (!archived) R.string.chat_was_unarchived else R.string.chat_was_archived,
                Snackbar.LENGTH_LONG
            )
        }

        snackbar?.setAction(
            R.string.undo
        ) {
            abstractChat.isArchived = !archived
            onStateSelected(previousState)
            updateRequest.onNext(null)
        }

        snackbar?.setActionTextColor(Color.YELLOW)
        snackbar?.show()
    }

    enum class ChatListState {
        RECENT, UNREAD, ARCHIVED
    }

    interface ChatListFragmentListener {
        fun onChatClick(contact: AbstractContact?)
        fun onChatListStateChanged(chatListState: ChatListState?)
        fun onChatListUpdated()
        fun onManageAccountsClick()
        fun onContactsClick()
    }

    enum class ChatListAvatarState {
        NOT_SPECIFIED, SHOW_AVATARS, DO_NOT_SHOW_AVATARS
    }

    init {
        updateRequest
            .debounce(1000, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { update() }
    }

}