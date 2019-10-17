package com.xabber.android.presentation.ui.contactlist;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
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
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.NewIncomingMessageEvent;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.mvp.contactlist.ContactListPresenter;
import com.xabber.android.presentation.mvp.contactlist.UpdateBackpressure;
import com.xabber.android.presentation.ui.contactlist.viewobjects.CategoryVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ChatVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ExtContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.ConferenceSelectActivity;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.SearchActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListGroupUtils;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.widget.ShortcutBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class ChatListFragment extends Fragment implements ContactVO.ContactClickListener,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemSwipeListener, View.OnClickListener,
        OnContactChangedListener, OnAccountChangedListener, UpdateBackpressure.UpdatableObject,
        PopupMenu.OnMenuItemClickListener {

    private UpdateBackpressure updateBackpressure;
    private FlexibleAdapter<IFlexible> adapter;
    private List<IFlexible> items;
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

    public interface ChatListFragmentListener{
        void onChatClick(AbstractContact contact);
        void onChatListStateChanged(ChatListState chatListState);
        void onUnreadChanged(int unread);
        void onManageAccountsClick();
    }

    @Override
    public void onAttach(Context context) {
        chatListFragmentListener = (ChatListFragmentListener) context;
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        EventBus.getDefault().register(this);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
        updateBackpressure.refreshRequest();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(NewIncomingMessageEvent event){
        playMessageSound();
        MessageNotificationManager.getInstance().removeAllMessageNotifications();
        updateBackpressure.refreshRequest();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageUpdateEvent event) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void onDetach() {
        chatListFragmentListener = null;
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class,this);
        EventBus.getDefault().unregister(this);
        updateBackpressure.removeRefreshRequests();
        super.onDetach();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        updateUnreadCount();
        if (getUnreadCount() == 0){
            onStateSelected(ChatListState.recent);
        }
        updateBackpressure.refreshRequest();
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
        updateBackpressure.run();
        chatListFragmentListener.onChatListStateChanged(state);
        toolbarAppBarLayout.setExpanded(true, false);
        this.closeSnackbar();
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
        markAllAsReadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                for (AbstractChat chat : MessageManager.getInstance().getChatsOfEnabledAccount()){
                    chat.markAsReadAll(true);
                }
                onStateSelected(ChatListFragment.ChatListState.recent);
                Toast.makeText(getActivity(), "All mesages were marked as read", Toast.LENGTH_SHORT).show();
            }
        });
        markAllReadBackground = view.getResources().getDrawable(R.drawable.unread_button_background);
        if (Build.VERSION.SDK_INT >= 21) markAllAsReadButton.setElevation(2);
        if (Build.VERSION.SDK_INT >= 16) markAllAsReadButton.setBackground(markAllReadBackground);
        placeholderView = view.findViewById(R.id.chatlist_placeholder_view);
        placeholderMessage = view.findViewById(R.id.chatlist_placeholder_message);
        placeholderButton = view.findViewById(R.id.chatlist_placeholder_button);

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, null, false);
        recyclerView.setAdapter(adapter);
        adapter.setDisplayHeadersAtStartUp(true);
        adapter.setSwipeEnabled(true);
        adapter.expandItemsAtStartUp();
        adapter.setStickyHeaders(true);
        adapter.addListener(this);
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
        if (!getActivity().getClass().getSimpleName().equals(ContactListActivity.class.getSimpleName()))
            toolbarAppBarLayout.setVisibility(View.GONE);

        /* Find possible max recycler items*/
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dpHeight = Math.round(displayMetrics.heightPixels / displayMetrics.density);
        maxItemsOnScreen = Math.round((dpHeight - 56 - 56) / 64);

        /* Initialize and run UpdateBackpressure */
        updateBackpressure = new UpdateBackpressure(this);
        updateBackpressure.run();
        return view;
    }

    /** Update toolbarRelativeLayout via current state */
    public void updateToolbar(){
        /* Update ChatState TextView display via current chat state */
        switch (currentChatsState) {
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

        /* Update background color via current main user; */
        TypedValue typedValue = new TypedValue();
        TypedArray a = getContext().obtainStyledAttributes(typedValue.data, new int[] {R.attr.contact_list_account_group_background});
        final int accountGroupColorsResourceId = a.getResourceId(0, 0);
        a.recycle();
        final int[] accountGroupColors = getContext().getResources().getIntArray(accountGroupColorsResourceId);
        final int level = AccountManager.getInstance().getColorLevel(AccountPainter.getFirstAccount());
        toolbarRelativeLayout.setBackgroundColor(accountGroupColors[level]);

        /* Update left color indicator via current main user */
        toolbarAccountColorIndicator.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        toolbarAccountColorIndicatorBack.setBackgroundColor(
                ColorManager.getInstance().getAccountPainter().getDefaultIndicatorBackColor());
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
        updateBackpressure.refreshRequest();
    }

    /**
    Update chat items in adapter
     */
    private void updateItems(List<IFlexible> items){
        CommonState commonState = AccountManager.getInstance().getCommonState();
        /* Show placeholder if necessary */
        if (commonState != CommonState.online){
            switch (commonState){
                case roster:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_connecting), null);
                    break;
                case connecting:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_connecting), null);
                    break;
                case waiting:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_waiting)
                            , Application.getInstance().getApplicationContext().getString(R.string.application_action_waiting));
                    placeholderButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ConnectionManager.getInstance().connectAll();
                        }
                    });
                    break;
                case offline:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_offline)
                            , Application.getInstance().getApplicationContext().getString(R.string.application_state_offline));
                    placeholderButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            AccountManager.getInstance().setStatus(StatusMode.available, null);
                        }
                    });
                    break;
                case disabled:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_disabled)
                            , Application.getInstance().getApplicationContext().getString(R.string.application_action_disabled));
                    placeholderButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            chatListFragmentListener.onManageAccountsClick();
                        }
                    });
                    break;
                case empty:
                    showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_empty),
                            Application.getInstance().getApplicationContext().getString(R.string.application_action_empty));
                    placeholderButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(AccountAddActivity.createIntent(getActivity()));
                        }
                    });
                    break;
            }
        } else if (items.size() == 0) {
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
        this.items.clear();
        this.items.addAll(items);
        adapter.updateDataSet(this.items);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        updateBackpressure.refreshRequest();
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
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void onItemSwipe(int position, int direction) {
        Object itemAtPosition = adapter.getItem(position);
        if (itemAtPosition != null && itemAtPosition instanceof ChatVO) {

            // backup of removed item for undo purpose
            final ChatVO deletedItem = (ChatVO) itemAtPosition;

            // update value
            setChatArchived(deletedItem, !(deletedItem).isArchived());

            // remove the item from recycler view
            adapter.removeItem(position);

            // showing snackbar with Undo option
            showSnackbar(deletedItem, position);

            // update unread count
            updateUnreadCount();
        }
    }

    @Override
    public void onContactAvatarClick(int adapterPosition) {
        IFlexible item = adapter.getItem(adapterPosition);
        if (item != null && item instanceof ContactVO) {
            Intent intent;
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            UserJid userJid = ((ContactVO) item).getUserJid();
            if (MUCManager.getInstance().hasRoom(accountJid, userJid)) {
                intent = ContactActivity.createIntent(getActivity(), accountJid, userJid);
            } else {
                intent = ContactEditActivity.createIntent(getActivity(), accountJid, userJid);
            }
            getActivity().startActivity(intent);
        }
    }

    @Override
    public void onContactCreateContextMenu(int adapterPosition, ContextMenu menu) {
        onItemContextMenu(adapterPosition, menu);
    }

    public void onItemContextMenu(int adapterPosition, ContextMenu menu){
        IFlexible item = adapter.getItem(adapterPosition);
        if (item != null && item instanceof ContactVO) {
            Intent intent;
            AccountJid accountJid = ((ContactVO) item).getAccountJid();
            UserJid userJid = ((ContactVO) item).getUserJid();
            if (MUCManager.getInstance().hasRoom(accountJid, userJid)) {
                intent = ContactActivity.createIntent(getActivity(), accountJid, userJid);
            } else {
                intent = ContactEditActivity.createIntent(getActivity(), accountJid, userJid);
            }
            getActivity().startActivity(intent);
        }
    }

    @Override
    public void onContactButtonClick(int adapterPosition) {
        //TODO
    }

    public void setChatArchived(ChatVO chatVO, boolean archived) {
        AbstractChat chat = MessageManager.getInstance().getChat(chatVO.getAccountJid(), chatVO.getUserJid());
        if (chat != null) chat.setArchived(archived, true);
    }

    public int getUnreadCount(){
        int unreadMessageCount = 0;
        for (AbstractChat abstractChat : MessageManager.getInstance().getChatsOfEnabledAccount()) {
            if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived())
                unreadMessageCount += abstractChat.getUnreadMessageCount();
        }
        return unreadMessageCount;
    }

    public void updateUnreadCount() {
        EventBus.getDefault().post(new ContactListPresenter.UpdateUnreadCountEvent(getUnreadCount()));
        chatListFragmentListener.onUnreadChanged(getUnreadCount());
    }

    @Override
    public boolean onItemClick(int position) {
        adapter.notifyItemChanged(position);
        IFlexible item = adapter.getItem(position);
        if (item instanceof  ContactVO){
            AccountJid accountJid = ((ContactVO)item).getAccountJid();
            UserJid userJid = ((ContactVO) item).getUserJid();
            chatListFragmentListener.onChatClick(RosterManager.getInstance().getAbstractContact(accountJid, userJid));
        }
        return true;
    }

    @Override
    public void update(){
        /* List for store final method result */
        List<IFlexible> items = new ArrayList<>();

        /* Map of accounts*/
        final Map<AccountJid, AccountConfiguration> accounts = new TreeMap<>();
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            accounts.put(account, null);
        }

        /* If filterString is empty, build regular chat list */
        if (filterString == null || filterString.equals("")){
            Collection<AbstractChat> chats = MessageManager.getInstance().getChatsOfEnabledAccount();
            final GroupConfiguration chatsGroup = getChatsGroup(chats, currentChatsState);
            items.clear();
            if (!chatsGroup.isEmpty()) {
                if (currentChatsState == ChatListState.recent){
                    for (AbstractContact contact : chatsGroup.getAbstractContacts()) {
                        items.add(ChatVO.convert(contact, this, null));
                    }
                } else {
                    for (AbstractContact contact : chatsGroup.getAbstractContacts()) {
                        items.add(ChatVO.convert(contact, this, null));
                    }
                }
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
            items.clear();
            items.add(new CategoryVO(Application.getInstance().getApplicationContext().getString(R.string.category_title_contacts)));
            items.addAll(SettingsManager.contactsShowMessages()
                    ? ExtContactVO.convert(baseEntities, this)
                    : ContactVO.convert(baseEntities, this));
        }

        /* Mark all the read button showing */
        if (currentChatsState == ChatListState.unread && items.size() > 0){
            markAllReadBackground.setColorFilter(ColorManager.getInstance().getAccountPainter().getDefaultMainColor(), PorterDuff.Mode.SRC_ATOP);
            markAllAsReadButton.setVisibility(View.VISIBLE);
        }
        else markAllAsReadButton.setVisibility(View.GONE);

        /* Update another elements */
        updateToolbar();
        updateUnreadCount();
        updateItems(items);
        setupToolbarLayout();
    }

    private GroupConfiguration getChatsGroup(Collection<AbstractChat> chats, ChatListState state) {
        GroupConfiguration chatsGroup = new GroupConfiguration(GroupManager.NO_ACCOUNT,
                GroupVO.RECENT_CHATS_TITLE, GroupManager.getInstance());
        List<AbstractChat> newChats = new ArrayList<>();
        for (AbstractChat abstractChat : chats) {
            MessageItem lastMessage = abstractChat.getLastMessage();
            if (lastMessage != null) {
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
        ShortcutBuilder.updateShortcuts(Application.getInstance(),
                new ArrayList<>(chatsGroup.getAbstractContacts()));
        return chatsGroup;
    }

    /** Returns an ArrayList of Contacts filtered by filterString **/
    private ArrayList<AbstractContact> getSearchResults(Collection<RosterContact> rosterContacts,
                                                        Map<AccountJid, Map<UserJid, AbstractChat>> abstractChats) {
        final ArrayList<AbstractContact> baseEntities = new ArrayList<>();

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
            || rosterContact.getUser().toString().toLowerCase(Locale.getDefault()).contains(filterString)) {
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
                || abstractContact.getUser().toString().toLowerCase(Locale.getDefault()).contains(filterString)) {
                    baseEntities.add(abstractContact);
                }
            }
        }
        Collections.sort(baseEntities, new ComparatorBySubstringPosition(filterString));
        return baseEntities;
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

    public enum ChatListState {
        recent,
        unread,
        archived,
        all
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

    public void showSnackbar(final ChatVO deletedItem, final int deletedIndex) {
        if (snackbar != null) snackbar.dismiss();
        final boolean archived = (deletedItem).isArchived();
        snackbar = Snackbar.make(coordinatorLayout, archived ? R.string.chat_was_unarchived
                : R.string.chat_was_archived, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // update value
                setChatArchived((ChatVO) deletedItem, archived);

                // undo is selected, restore the deleted item
                adapter.addItem(deletedIndex, deletedItem);

                // update unread count
                updateUnreadCount();
            }
        });
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    public void closeSnackbar() {
        if (snackbar != null) snackbar.dismiss();
    }

    @Override
    public void onActionStateChanged(RecyclerView.ViewHolder viewHolder, int actionState) { }
}