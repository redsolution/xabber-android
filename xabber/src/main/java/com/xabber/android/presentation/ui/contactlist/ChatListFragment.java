package com.xabber.android.presentation.ui.contactlist;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.database.DatabaseManager;
import com.xabber.android.data.database.realmobjects.MessageItem;
import com.xabber.android.data.database.repositories.MessageRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.OnChatStateListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.ui.activity.ConferenceSelectActivity;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.ContactViewerActivity;
import com.xabber.android.ui.activity.SearchActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.chatListFragment.ChatItemDiffUtil;
import com.xabber.android.ui.fragment.chatListFragment.ChatListAdapter;
import com.xabber.android.ui.fragment.chatListFragment.ChatListItemListener;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.widget.ShortcutBuilder;
import com.xabber.android.utils.StringUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class ChatListFragment extends Fragment implements ChatListItemListener, View.OnClickListener,
        OnChatStateListener, PopupMenu.OnMenuItemClickListener, ContextMenuHelper.ListPresenter {

    private ChatListAdapter adapter;
    private List<AbstractContact> items;
    private Snackbar snackbar;
    private CoordinatorLayout coordinatorLayout;
    private LinearLayoutManager linearLayoutManager;
    private ChatListFragmentListener chatListFragmentListener;
    private ChatListState currentChatsState = ChatListState.recent;
    private RecyclerView recyclerView;
    private TextView markAllAsReadButton;
    private Drawable markAllReadBackground;
    private String filterString;

    private int maxItemsOnScreen;

    /* Placeholder variables */
    private View placeholderView;
    private TextView placeholderMessage;
    private Button placeholderButton;
    private int showPlaceholders;

    /* Toolbar variables */
    private RelativeLayout toolbarRelativeLayout;
    private AppBarLayout toolbarAppBarLayout;
    private Toolbar toolbarToolbarLayout;
    private View toolbarAccountColorIndicator;
    private View toolbarAccountColorIndicatorBack;
    private ImageView toolbarAddIv;
    private TextView toolbarTitleTv;
    private ImageView toolbarAvatarIv;
    private ImageView toolbarStatusIv;
    private ImageView toolbarSearchIv;

    private Subscription realmChangeListenerSubscription;

    public interface ChatListFragmentListener{
        void onChatClick(AbstractContact contact);
        void onChatListStateChanged(ChatListState chatListState);
        void onUnreadChanged(int unread);
    }

    @Override
    public void onAttach(Context context) {
        chatListFragmentListener = (ChatListFragmentListener) context;
        chatListFragmentListener.onChatListStateChanged(currentChatsState);
        super.onAttach(context);
    }

    public void playMessageSound() {
        if (!SettingsManager.eventsInChatSounds()) return;

        final MediaPlayer mp;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build();
            mp = MediaPlayer.create(getActivity(), R.raw.message_alert,
                    attr, AudioManager.AUDIO_SESSION_ID_GENERATE);
        } else {
            mp = MediaPlayer.create(getActivity(), R.raw.message_alert);
            mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
        }

        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    @Override
    public void onDetach() {
        chatListFragmentListener = null;
        super.onDetach();
    }

    @Override
    public void onStop() {
        if (realmChangeListenerSubscription != null) realmChangeListenerSubscription.unsubscribe();
        Application.getInstance().removeUIListener(OnChatStateListener.class, this);
        super.onStop();
    }

    @Override
    public void onResume() {
        Application.getInstance().addUIListener(OnChatStateListener.class, this);
        if (MessageRepository.getAllUnreadMessagesCount() == 0){
            onStateSelected(ChatListState.recent);
        }

        realmChangeListenerSubscription = DatabaseManager.getInstance().getObservableListener()
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> LogManager.exception("ChatListFragment", throwable))
                .subscribe(realm -> {
                    try {update();} catch (Exception e) {LogManager.exception("ChatList", e);}
                });

        update();

        super.onResume();
    }

    public static ChatListFragment newInstance(@Nullable AccountJid account){
        ChatListFragment fragment = new ChatListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable("account_jid", account);
        fragment.setArguments(args);
        return fragment;
    }

    public void onStateSelected(ChatListState state) {
        this.currentChatsState = state;
        chatListFragmentListener.onChatListStateChanged(state);
        toolbarAppBarLayout.setExpanded(true, false);
        update();
        closeSnackbar();
    }

    public ChatListState getCurrentChatsState(){
        return currentChatsState;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO implement scroll to account if it need;
    }

    public void scrollToTop(){
        if (recyclerView != null && recyclerView.getAdapter().getItemCount() != 0){
            recyclerView.scrollToPosition(0);
            toolbarAppBarLayout.setExpanded(true, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.chatlist_recyclerview);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.chatlist_coordinator_layout);
        markAllAsReadButton = (TextView) view.findViewById(R.id.mark_all_as_read_button);
        markAllReadBackground = view.getResources().getDrawable(R.drawable.unread_button_background);
        if (Build.VERSION.SDK_INT >= 21) markAllAsReadButton.setElevation(2);
        if (Build.VERSION.SDK_INT >= 16) markAllAsReadButton.setBackground(markAllReadBackground);
        placeholderView = view.findViewById(R.id.chatlist_placeholder_view);
        placeholderMessage = view.findViewById(R.id.chatlist_placeholder_message);
        placeholderButton = view.findViewById(R.id.chatlist_placeholder_button);

        items = new ArrayList<>();
        adapter = new ChatListAdapter(items, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        MessageNotificationManager.getInstance().removeAllMessageNotifications();
        chatListFragmentListener.onChatListStateChanged(currentChatsState);

        /* Toolbar variables initialization */
        toolbarRelativeLayout = view.findViewById(R.id.toolbar_chatlist);
        toolbarToolbarLayout = view.findViewById(R.id.chat_list_toolbar);
        toolbarAccountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        toolbarAccountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
        toolbarAddIv = (ImageView) view.findViewById(R.id.ivAdd);
        toolbarTitleTv = (TextView) view.findViewById(R.id.tvTitle);
        toolbarAvatarIv = (ImageView) view.findViewById(R.id.ivAvatar);
        toolbarStatusIv = (ImageView) view.findViewById(R.id.ivStatus);
        toolbarSearchIv = (ImageView) view.findViewById(R.id.toolbar_search_button);
        toolbarAppBarLayout = view.findViewById(R.id.chatlist_toolbar_root);
        toolbarAddIv.setOnClickListener(this);
        toolbarAvatarIv.setOnClickListener(this);
        toolbarTitleTv.setOnClickListener(this);
        toolbarSearchIv.setOnClickListener(this);
        toolbarTitleTv.setText(Application.getInstance().getApplicationContext().getString(R.string.account_state_connecting));
        if (!getActivity().getClass().getSimpleName().equals(ContactListActivity.class.getSimpleName()))
            toolbarAppBarLayout.setVisibility(View.GONE);

        /* Find possible max recycler items*/
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dpHeight = Math.round(displayMetrics.heightPixels / displayMetrics.density);
        maxItemsOnScreen = Math.round((dpHeight - 56 - 56) / 64);
        showPlaceholders = 0;

        return view;
    }

    @Override
    public void updateContactList() {
        update();
    }

    /** Update toolbarRelativeLayout via current state */
    public void updateToolbar(){
        /* Update ChatState TextView display via current chat and connection state */
        if (AccountManager.getInstance().getCommonState() == CommonState.connecting)
            toolbarTitleTv.setText(Application.getInstance().getApplicationContext().getString(R.string.account_state_connecting));
        else switch (currentChatsState) {
            case unread:
                toolbarTitleTv.setText(R.string.unread_chats);
                break;
            case archived:
                toolbarTitleTv.setText(R.string.archived_chats);
                break;
            case all:
                toolbarTitleTv.setText(R.string.all_chats);
                break;
            default:
                toolbarTitleTv.setText("Xabber");
                break;
        }

        /* Update avatar and status ImageViews via current settings and main user */
        if (SettingsManager.contactsShowAvatars() && AccountManager.getInstance().getEnabledAccounts().size() != 0){
            toolbarAvatarIv.setVisibility(View.VISIBLE);
            toolbarStatusIv.setVisibility(View.VISIBLE);
            AccountJid mainAccountJid = AccountPainter.getFirstAccount();
            AccountItem mainAccountItem = AccountManager.getInstance().getAccount(mainAccountJid);
            Drawable mainAccountAvatar = AvatarManager.getInstance().getAccountAvatar(mainAccountJid);
            int mainAccountStatusMode = mainAccountItem.getDisplayStatusMode().getStatusLevel();
            toolbarAvatarIv.setImageDrawable(mainAccountAvatar);
            toolbarStatusIv.setImageLevel(mainAccountStatusMode);
        } else {
            toolbarAvatarIv.setVisibility(View.GONE);
            toolbarStatusIv.setVisibility(View.GONE);
        }

        /* Update background color via current main user and theme; */

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light &&
                AccountPainter.getFirstAccount() != null)
            toolbarRelativeLayout.setBackgroundColor(ColorManager.getInstance().getAccountPainter().
                    getAccountRippleColor(AccountPainter.getFirstAccount()));
        else if (getContext() != null){
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.bars_color, typedValue, true);
            toolbarRelativeLayout.setBackgroundColor(typedValue.data);
        }

        /* Update left color indicator via current main user */
        if (AccountManager.getInstance().getEnabledAccounts().size() > 1){
            toolbarAccountColorIndicator.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
            toolbarAccountColorIndicatorBack.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultIndicatorBackColor());
        } else {
            toolbarAccountColorIndicator.setBackgroundColor(Color.TRANSPARENT);
            toolbarAccountColorIndicatorBack.setBackgroundColor(Color.TRANSPARENT);
        }
        setupToolbarLayout();
    }

    /** OnClickListener for Toolbar */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivAdd:
                showToolbarPopup(toolbarAddIv);
                break;
            case R.id.ivAvatar:
                startActivity(StatusEditActivity.createIntent(getActivity()));
                break;
            case R.id.tvTitle:
                showTitlePopup(toolbarTitleTv);
                break;
            case R.id.toolbar_search_button:
                startActivity(SearchActivity.createSearchIntent(getActivity()));
                break;
        }
    }

    /** @return  Return true when first element on the top of list*/
    public boolean isOnTop(){
        return linearLayoutManager.findLastCompletelyVisibleItemPosition() == 0;
    }

    /** @return Size of list */
    public int getListSize(){ return items.size(); }

    /** Show menu Add contact / Add conference */
    private void showToolbarPopup(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_add_in_contact_list);
        popupMenu.show();
    }

    /** Show menu Chat state */
    private void showTitlePopup(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_chat_list);
        popupMenu.show();
    }

    /** Handle toolbarRelativeLayout menus clicks */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_contact:
                startActivity(ContactAddActivity.createIntent(getActivity()));
                return true;
            case R.id.action_join_conference:
                startActivity(ConferenceSelectActivity.createIntent(getActivity()));
                return true;
            case R.id.action_recent_chats:
                onStateSelected(ChatListFragment.ChatListState.recent);
                return true;
            case R.id.action_unread_chats:
                onStateSelected(ChatListFragment.ChatListState.unread);
                return true;
            case R.id.action_archived_chats:
                onStateSelected(ChatListFragment.ChatListState.archived);
                return true;
            default:
                return false;
        }
    }

    /** Show contacts filtered by filterString */
    public void search(String filterString){
        this.filterString = filterString;
        //TODO implement search
    }

    /**
    Update chat items in adapter
     */
    private void updateItems(List<AbstractContact> newItems){
         if (newItems.size() == 0 && showPlaceholders >= 3) {
            switch (currentChatsState) {
                case unread:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.placeholder_no_unread), null);
                    break;
                case archived:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.placeholder_no_archived), null);
                    break;
                default:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_no_contacts),
                            Application.getInstance().getApplicationContext().getString(R.string.application_action_no_contacts));
                    placeholderButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(ContactAddActivity.createIntent(getActivity()));
                        }
                    });
                    break;
            }
        } else hidePlaceholder();

        /* Update items in RecyclerView */
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatItemDiffUtil(items, newItems, adapter), false);
        items.clear();
        items.addAll(newItems);
        adapter.addItems(newItems);
        diffResult.dispatchUpdatesTo(adapter);
    }

    @Override
    public void onChatStateChanged(Collection<RosterContact> entities) {
        update();
    }

    /**
     * Setup Toolbar scroll behavior according to count of visible chat items
     */
    public void setupToolbarLayout(){
        if (recyclerView != null){
            int count = items.size();
            if (count <= maxItemsOnScreen){
                setToolbarScrollEnabled(false);
            } else {    setToolbarScrollEnabled(true);  }
        }
    }

    /**
     * Enable or disable Toolbar scroll behavior
     * @param enabled
     */
    private void setToolbarScrollEnabled(boolean enabled){
        AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) toolbarToolbarLayout.getLayoutParams();
        CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) toolbarAppBarLayout.getLayoutParams();
        if (enabled && toolbarLayoutParams.getScrollFlags() == 0){
            appBarLayoutParams.setBehavior(new AppBarLayout.Behavior());
            toolbarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        } else if (!enabled && toolbarLayoutParams.getScrollFlags() != 0) {
            toolbarLayoutParams.setScrollFlags(0);
            appBarLayoutParams.setBehavior(null);
        }
        toolbarToolbarLayout.setLayoutParams(toolbarLayoutParams);
        toolbarAppBarLayout.setLayoutParams(appBarLayoutParams);
    }

    @Override
    public void onChatItemSwiped(@NotNull AbstractContact abstractContact) {
        AbstractChat abstractChat = MessageManager.getInstance()
                .getChat(abstractContact.getAccount(), abstractContact.getUser());
        MessageManager.getInstance().getChat(abstractContact.getAccount(), abstractContact.getUser())
                .setArchived(!abstractChat.isArchived(), true);
        showSnackbar(abstractContact, currentChatsState);
    }

    @Override
    public void onChatAvatarClick(AbstractContact item) {
        Intent intent;
        AccountJid accountJid = item.getAccount();
        UserJid userJid = item.getUser();
        if (MUCManager.getInstance().hasRoom(accountJid, userJid)) {
            intent = ContactActivity.createIntent(getActivity(), accountJid, userJid);
        } else {
            intent = ContactViewerActivity.createIntent(getActivity(), accountJid, userJid);
        }
        getActivity().startActivity(intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    public void onChatItemContextMenu(ContextMenu menu, AbstractContact contact){
        AccountJid accountJid = contact.getAccount();
        UserJid userJid = contact.getUser();
        AbstractContact abstractContact = RosterManager.getInstance().getAbstractContact(accountJid, userJid);
        ContextMenuHelper.createContactContextMenu(getActivity(), this, abstractContact, menu);
    }

    @Override
    public void onListBecomeEmpty() {
        if (currentChatsState != ChatListState.recent) currentChatsState = ChatListState.recent;
            update();
    }

    public void updateUnreadCount() {
//        int unreadCount = 0;
//        for (AbstractChat abstractChat : MessageManager.getInstance().getChatsOfEnabledAccount())
//            if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived())
//                unreadCount += abstractChat.getUnreadMessageCount();
//        if (chatListFragmentListener != null)
//            chatListFragmentListener.onUnreadChanged(unreadCount);
    }

    @Override
    public void onChatItemClick(AbstractContact item) {
        AccountJid accountJid = item.getAccount();
        UserJid userJid = item.getUser();
        chatListFragmentListener.onChatClick(RosterManager.getInstance().getAbstractContact(accountJid, userJid));
    }

    public void update(){

        /* List for store final method result */
        List<AbstractContact> newList = new ArrayList<>();
        showPlaceholders++;
        /* Map of accounts*/
        final Map<AccountJid, AccountConfiguration> accounts = new TreeMap<>();
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            accounts.put(account, null);
        }

        /* If filterString is empty, build regular chat list */
        if (filterString == null || filterString.equals("")){
            final GroupConfiguration chatsGroup = getChatsGroup(currentChatsState);
            newList.clear();
            if (!chatsGroup.isEmpty()) {
                newList.addAll(chatsGroup.getAbstractContacts());

            }
        } else {
            /* If filterString not empty, perform a search */

            /*  Make list of rooms and active chats grouped by users inside accounts */
            final Map<AccountJid, Map<UserJid, AbstractChat>> abstractChats = new TreeMap<>();
            for (AbstractChat abstractChat : MessageManager.getInstance().getChats()) {
                if ((abstractChat instanceof RoomChat || abstractChat.isActive())
                        && accounts.containsKey(abstractChat.getAccount())) {
                    final AccountJid account = abstractChat.getAccount();
                    Map<UserJid, AbstractChat> users = abstractChats.get(account);
                    if (users == null) {
                        users = new TreeMap<>();
                        abstractChats.put(account, users);
                    }
                    users.put(abstractChat.getUser(), abstractChat);
                }
            }
            /* All roster contact collection */
            final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getAllContacts();
            /* Map with blocked contacts for accounts */
            Map<AccountJid, Collection<UserJid>> blockedContacts = new TreeMap<>();
            for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
                blockedContacts.put(account, BlockingManager.getInstance().getCachedBlockedContacts(account));
            }
            /* Filter all blocked contacts from allRosterContacts and save result to rosterContacts*/
            final Collection<RosterContact> rosterContacts = new ArrayList<>();
            for (RosterContact contact : allRosterContacts) {
                if (blockedContacts.containsKey(contact.getAccount())) {
                    Collection<UserJid> blockedUsers = blockedContacts.get(contact.getAccount());
                    if (blockedUsers != null) {
                        if (!blockedUsers.contains(contact.getUser()))
                            rosterContacts.add(contact);
                    } else rosterContacts.add(contact);
                } else rosterContacts.add(contact);
            }
            final ArrayList<AbstractContact> baseEntities = getSearchResults(rosterContacts, abstractChats);
            newList.clear();
            newList.addAll(baseEntities);
        }

        setupMarkAllTheReadButton(newList.size());

        /* Update another elements */
        updateUnreadCount();
        updateItems(newList);
        updateToolbar();
    }

    private void setupMarkAllTheReadButton(int listSize){
        if (currentChatsState == ChatListState.unread && listSize > 0 && getContext() != null){
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light){
                markAllReadBackground.setColorFilter(ColorManager.getInstance().getAccountPainter()
                        .getDefaultMainColor(), PorterDuff.Mode.SRC_ATOP);
                markAllAsReadButton.setTextColor(getContext().getResources().getColor(R.color.white));
            } else {
                markAllReadBackground.setColorFilter(getContext().getResources().getColor(R.color.grey_900), PorterDuff.Mode.SRC_ATOP);
                markAllAsReadButton.setTextColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
            }

            markAllAsReadButton.setVisibility(View.VISIBLE);
            markAllAsReadButton.setOnClickListener(v -> {
                for (AbstractContact abstractContact : getChatsGroup(ChatListState.recent).getAbstractContacts()){
                    MessageManager.getInstance().getChat(abstractContact.getAccount(), abstractContact.getUser()).markAsReadAll(true);
                }
                onStateSelected(ChatListFragment.ChatListState.recent);
                Toast toast = Toast.makeText(getActivity(), R.string.all_chats_were_market_as_read_toast, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, (int)(getResources().getDimension(R.dimen.bottom_navigation_height) * 1.2f));
                toast.show();
            });
        }
        else markAllAsReadButton.setVisibility(View.GONE);
    }

    private GroupConfiguration getChatsGroup(ChatListState state) {
        Collection<AbstractChat> chats = MessageManager.getInstance().getChatsOfEnabledAccount();
        GroupConfiguration chatsGroup = new GroupConfiguration(GroupManager.NO_ACCOUNT,
                GroupVO.RECENT_CHATS_TITLE, GroupManager.getInstance());
        List<AbstractChat> newChats = new ArrayList<>();
        for (AbstractChat abstractChat : chats) {
            MessageItem lastMessage = abstractChat.getLastMessage();
            if (lastMessage != null || abstractChat.getChatstateMode() == AbstractChat.ChatstateType.CLEARED_HISTORY) {
                switch (state) {
                    case unread:
                        if (!abstractChat.isArchived() && abstractChat.getUnreadMessageCount() > 0)
                            newChats.add(abstractChat);
                        break;
                    case archived:
                        if (abstractChat.isArchived()) newChats.add(abstractChat);
                        break;
                    default:
                        // recent
                        if (!abstractChat.isArchived()) newChats.add(abstractChat);
                        break;
                }
            }
        }
        Collections.sort(newChats, ChatComparator.CHAT_COMPARATOR);
        chatsGroup.setNotEmpty();
        for (AbstractChat chat : newChats) {
            chatsGroup.addAbstractContact(RosterManager.getInstance() .getBestContact(chat.getAccount(), chat.getUser()));
            chatsGroup.increment(true);
        }
        Application.getInstance().runInBackground(() -> {ShortcutBuilder.updateShortcuts(Application.getInstance(),
                new ArrayList<>(chatsGroup.getAbstractContacts()));});
        return chatsGroup;
    }

    /** Returns an ArrayList of Contacts filtered by filterString **/
    private ArrayList<AbstractContact> getSearchResults(Collection<RosterContact> rosterContacts,
                                                        Map<AccountJid, Map<UserJid, AbstractChat>> abstractChats) {
        final ArrayList<AbstractContact> baseEntities = new ArrayList<>();
        String transliterated = StringUtils.translitirateToLatin(filterString);
        // Build structure.
        for (RosterContact rosterContact : rosterContacts) {
            if (!rosterContact.isEnabled()) {
                continue;
            }
            final AccountJid account = rosterContact.getAccount();
            final Map<UserJid, AbstractChat> users = abstractChats.get(account);
            if (users != null) {
                users.remove(rosterContact.getUser());
            }
            if (rosterContact.getName().toLowerCase(Locale.getDefault()).contains(filterString)
                    || rosterContact.getName().toString().toLowerCase(Locale.getDefault()).contains(transliterated)
                    || rosterContact.getUser().toString().toLowerCase(Locale.getDefault()).contains(filterString)
                    || rosterContact.getUser().toString().toLowerCase(Locale.getDefault()).contains(transliterated)) {
                baseEntities.add(rosterContact);
            }
        }
        for (Map<UserJid, AbstractChat> users : abstractChats.values()) {
            for (AbstractChat abstractChat : users.values()) {
                final AbstractContact abstractContact;
                if (abstractChat instanceof RoomChat) {
                    abstractContact = new RoomContact((RoomChat) abstractChat);
                } else {
                    abstractContact = new ChatContact(abstractChat);
                }
                if (abstractContact.getName().toLowerCase(Locale.getDefault()).contains(filterString)
                        || abstractContact.getUser().toString().toLowerCase(Locale.getDefault()).contains(filterString)
                        || abstractContact.getName().toLowerCase(Locale.getDefault()).contains(transliterated)
                        || abstractContact.getUser().toString().toLowerCase(Locale.getDefault()).contains(transliterated)){
                    baseEntities.add(abstractContact);
                }
            }
        }
        Collections.sort(baseEntities, new ComparatorBySubstringPosition(filterString));
        return baseEntities;
    }

    private void showPlaceholder(String message, @Nullable String buttonMessage){
        placeholderMessage.setText(message);
        if (buttonMessage != null){
            placeholderButton.setVisibility(View.VISIBLE);
            placeholderButton.setText(buttonMessage);
        }
        placeholderView.setVisibility(View.VISIBLE);
    }

    private void hidePlaceholder(){
        recyclerView.setVisibility(View.VISIBLE);
        placeholderView.setVisibility(View.GONE);
        placeholderButton.setVisibility(View.GONE);
    }

    private void showSnackbar(final AbstractContact deletedItem, final ChatListState previousState){
        if (snackbar != null) snackbar.dismiss();
        final AbstractChat abstractChat = MessageManager.getInstance().getChat(deletedItem.getAccount(), deletedItem.getUser());
        final boolean archived = abstractChat.isArchived();
        snackbar = Snackbar.make(coordinatorLayout, !archived ? R.string.chat_was_unarchived
                : R.string.chat_was_archived, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abstractChat.setArchived(!archived, true);
                onStateSelected(previousState);
                update();
            }
        });
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    private void closeSnackbar() {
        if (snackbar != null) snackbar.dismiss();
    }

    public enum ChatListState {
        recent,
        unread,
        archived,
        all
    }

    private class ComparatorBySubstringPosition implements Comparator<AbstractContact>{
        String substring;

        ComparatorBySubstringPosition(String substring){ this.substring = substring; }

        ComparatorBySubstringPosition(){ this.substring = null; }

        @Override
        public int compare(AbstractContact o1, AbstractContact o2) {
            String firstString = (o1.getName() + o1.getUser().toString()).toLowerCase();
            String secondString = (o2.getName() + o2.getUser().toString()).toLowerCase();
            int statusComparing = o1.getStatusMode().compareTo(o2.getStatusMode());
            int firstPosintion = firstString.indexOf(substring);
            int secondPosition = secondString.indexOf(substring);
            if (firstPosintion > secondPosition) return 1;
            if (firstPosintion < secondPosition) return -1;
            else if (statusComparing != 0) return statusComparing;
            else return (firstString.compareTo(secondString));
        }
    }

}