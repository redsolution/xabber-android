package com.xabber.android.ui.fragment.chatListFragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.IntDef;
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
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.android.data.notification.MessageNotificationManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnChatStateListener;
import com.xabber.android.data.roster.OnStatusChangeListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactViewerActivity;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.helper.ContextMenuHelper;
import com.xabber.android.ui.widget.DividerItemDecoration;
import com.xabber.android.utils.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ChatListFragment extends Fragment implements ChatListItemListener, View.OnClickListener,
        OnChatStateListener, PopupMenu.OnMenuItemClickListener, ContextMenuHelper.ListPresenter,
        OnStatusChangeListener, ChatListUpdateBackpressure.UpdatableObject {

    public static final int NOT_SPECIFIED = 0;
    public static final int SHOW_AVATARS = 1;
    public static final int DO_NOT_SHOW_AVATARS = 2;
    private ChatListUpdateBackpressure updateBackpressure;
    private ChatListAdapter adapter;
    private List<AbstractChat> items;
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

    public static ChatListFragment newInstance(@Nullable AccountJid account) {
        ChatListFragment fragment = new ChatListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable("account_jid", account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        chatListFragmentListener = (ChatListFragmentListener) context;
        chatListFragmentListener.onChatListStateChanged(currentChatsState);
        updateBackpressure = new ChatListUpdateBackpressure(this);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        chatListFragmentListener = null;
        super.onDetach();
    }

    @Override
    public void onStop() {
        Application.getInstance().removeUIListener(OnChatStateListener.class, this);
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);

        MessageNotificationManager.getInstance().setShowBanners(true);

        updateBackpressure.removeRefreshRequests();
    }

    @Override
    public void onResume() {

        updateBackpressure.build();

        Application.getInstance().addUIListener(OnChatStateListener.class, this);
        Application.getInstance().addUIListener(OnStatusChangeListener.class, this);

        MessageNotificationManager.getInstance().setShowBanners(false);

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        int unreadCount = 0;
        for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts())
            if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived())
                unreadCount += abstractChat.getUnreadMessageCount();
        if (unreadCount == 0) {
            currentChatsState = ChatListState.recent;
            chatListFragmentListener.onChatListStateChanged(ChatListState.recent);
        }
        update();

        super.onResume();
    }

    @Override
    public void onStatusChanged(AccountJid account, ContactJid user, String statusText) {
        updateBackpressure.refreshRequest();
    }

    @Override
    public void onStatusChanged(AccountJid account, ContactJid user, StatusMode statusMode, String statusText) {
        updateBackpressure.refreshRequest();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectionStateChanged(ConnectionItem.ConnectionStateChangedEvent connectionStateChangedEvent) {
        if (connectionStateChangedEvent.getConnectionState() == ConnectionState.connected
                || connectionStateChangedEvent.getConnectionState() == ConnectionState.disconnecting)
            update();
        else
            update();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChatsChanged(ChatManager.ChatUpdatedEvent chatUpdatedEvent) {
        update();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageChangedEvent(MessageUpdateEvent chatUpdatedEvent) {
        update();
    }

    public void onStateSelected(ChatListState state) {
        this.currentChatsState = state;
        chatListFragmentListener.onChatListStateChanged(state);
        toolbarAppBarLayout.setExpanded(true, false);
        update();
        closeSnackbar();
    }

    public ChatListState getCurrentChatsState() {
        return currentChatsState;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO implement scroll to account if it need;
    }

    public void scrollToTop() {
        if (recyclerView != null && recyclerView.getAdapter().getItemCount() != 0) {
            recyclerView.scrollToPosition(0);
            toolbarAppBarLayout.setExpanded(true, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        recyclerView = view.findViewById(R.id.chatlist_recyclerview);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        coordinatorLayout = view.findViewById(R.id.chatlist_coordinator_layout);
        markAllAsReadButton = view.findViewById(R.id.mark_all_as_read_button);
        markAllReadBackground = view.getResources().getDrawable(R.drawable.unread_button_background);
        if (Build.VERSION.SDK_INT >= 21) markAllAsReadButton.setElevation(2);
        if (Build.VERSION.SDK_INT >= 16) markAllAsReadButton.setBackground(markAllReadBackground);
        placeholderView = view.findViewById(R.id.chatlist_placeholder_view);
        placeholderMessage = view.findViewById(R.id.chatlist_placeholder_message);
        placeholderButton = view.findViewById(R.id.chatlist_placeholder_button);

        items = new ArrayList<>();
        adapter = new ChatListAdapter(items, this, true);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        DividerItemDecoration divider = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        divider.setChatListOffsetMode(SettingsManager.contactsShowAvatars() ? SHOW_AVATARS : DO_NOT_SHOW_AVATARS);
        recyclerView.addItemDecoration(divider);
        MessageNotificationManager.getInstance().removeAllMessageNotifications();
        chatListFragmentListener.onChatListStateChanged(currentChatsState);

        /* Toolbar variables initialization */
        toolbarRelativeLayout = view.findViewById(R.id.toolbar_chatlist);
        toolbarToolbarLayout = view.findViewById(R.id.chat_list_toolbar);
        toolbarAccountColorIndicator = view.findViewById(R.id.accountColorIndicator);
        toolbarAccountColorIndicatorBack = view.findViewById(R.id.accountColorIndicatorBack);
        toolbarAddIv = view.findViewById(R.id.ivAdd);
        toolbarTitleTv = view.findViewById(R.id.tvTitle);
        toolbarAvatarIv = view.findViewById(R.id.ivAvatar);
        toolbarStatusIv = view.findViewById(R.id.ivStatus);
        toolbarAppBarLayout = view.findViewById(R.id.chatlist_toolbar_root);
        toolbarTitleTv.setText(Application.getInstance().getApplicationContext().getString(R.string.account_state_connecting));
        toolbarAddIv.setOnClickListener(v -> startActivity(ContactAddActivity.createIntent(getActivity())));
        toolbarAvatarIv.setOnClickListener(this);
        toolbarTitleTv.setOnClickListener(this);
        if (!getActivity().getClass().getSimpleName().equals(MainActivity.class.getSimpleName()))
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
        updateBackpressure.refreshRequest();
    }

    /**
     * Update toolbarRelativeLayout via current state
     */
    private void updateToolbar() {
        /* Update ChatState TextView display via current chat and connection state */
        if (AccountManager.getInstance().getCommonState() == CommonState.online)
            toolbarTitleTv.setText(R.string.application_title_full);
        else switch (currentChatsState) {
            case unread:
                toolbarTitleTv.setText(R.string.unread_chats);
                break;
            case archived:
                toolbarTitleTv.setText(R.string.archived_chats);
                break;
            default:
                toolbarTitleTv.setText(R.string.account_state_connecting);
                break;
        }

        /* Update avatar and status ImageViews via current settings and main user */
        if (SettingsManager.contactsShowAvatars()) {
            toolbarAvatarIv.setVisibility(View.VISIBLE);
            toolbarStatusIv.setVisibility(View.VISIBLE);
            Drawable mainAccountDrawable = AvatarManager.getInstance().getMainAccountAvatar();
            if (mainAccountDrawable != null) toolbarAvatarIv.setImageDrawable(mainAccountDrawable);
            if (AccountManager.getInstance().getEnabledAccounts().size() > 0) {
                AccountJid accountJid = AccountManager.getInstance().getFirstAccount();
                int mainAccountStatusMode = StatusMode.unavailable.getStatusLevel();
                if (accountJid != null) {
                    mainAccountStatusMode = AccountManager.getInstance()
                            .getAccount(accountJid)
                            .getDisplayStatusMode()
                            .getStatusLevel();
                }
                toolbarStatusIv.setImageLevel(mainAccountStatusMode);
            } else {
                toolbarStatusIv.setImageLevel(StatusMode.unavailable.ordinal());
            }
        } else {
            toolbarAvatarIv.setVisibility(View.GONE);
            toolbarStatusIv.setVisibility(View.GONE);
        }

        /* Update background color via current main user and theme; */

        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
            toolbarRelativeLayout.setBackgroundColor(ColorManager.getInstance().getAccountPainter().
                    getDefaultRippleColor());
        } else if (getContext() != null) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(R.attr.bars_color, typedValue, true);
            toolbarRelativeLayout.setBackgroundColor(typedValue.data);
        }

        /* Update left color indicator via current main user */
        if (AccountManager.getInstance().getEnabledAccounts().size() > 1) {
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

    /**
     * OnClickListener for Toolbar
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivAdd:
                showToolbarPopup(toolbarAddIv);
                break;
            case R.id.ivAvatar:
                startActivity(AccountActivity.createIntent(getActivity(),
                        AccountManager.getInstance().getFirstAccount()));
                break;
            case R.id.tvTitle:
                showTitlePopup(toolbarTitleTv);
                break;
        }
    }

    private void testLayoutInflating() {
        final long optimizedDefault = getLayoutTime(R.layout.item_chat_in_contact_list);
        final long custom = getLayoutTime(R.layout.item_chat_in_contact_list_new);
        final long oldDefault = getLayoutTime(R.layout.item_chat_in_contact_list_old);

        LogManager.i("LayoutTime", "optimized default : " + optimizedDefault);
        LogManager.i("LayoutTime", "custom : " + custom);
        LogManager.i("LayoutTime", "old default : " + oldDefault);
    }

    private long getLayoutTime(int layoutRes) {
        final Context targetContext = recyclerView.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(targetContext);

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            final View view = layoutInflater.inflate(layoutRes, null);
            view.setLayoutParams(new ViewGroup.LayoutParams(0, 0));

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(
                            recyclerView.getWidth(),
                            View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                            (int) (getResources().getDisplayMetrics().density * 64),
                            View.MeasureSpec.UNSPECIFIED)
            );
            final int measuredHeight = view.getMeasuredHeight();
            final int measuredWidth = view.getMeasuredWidth();

            view.layout(0, 0, measuredWidth, measuredHeight);
        }
        return System.currentTimeMillis() - startTime;
    }

    /**
     * @return Return true when first element of chat list is on the top of the screen
     */
    public boolean isOnTop() {
        return linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0;
    }

    /**
     * @return Size of chat list
     */
    public int getListSize() {
        return items.size();
    }

    /**
     * Show menu Add contact / Add conference
     */
    private void showToolbarPopup(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_add_in_contact_list);
        popupMenu.show();
    }

    /**
     * Show menu Chat state
     */
    private void showTitlePopup(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.menu_chat_list);
        popupMenu.show();
    }

    /**
     * Handle toolbarRelativeLayout menus clicks
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
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

    /**
     * Show contacts filtered by filterString
     */
    public void search(String filterString) {
        this.filterString = filterString;
        update();
        //TODO implement search
    }

    /**
     * Update chat items in adapter
     */
    private void updateItems(List<AbstractChat> newItems) {
        boolean tempIsOnTop = isOnTop();
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
                    placeholderButton.setOnClickListener(
                            view -> startActivity(ContactAddActivity.createIntent(getActivity())));
                    break;
            }
        } else hidePlaceholder();

        /* Update items in RecyclerView */
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatItemDiffUtil(items,
                newItems, adapter), false);
        items.clear();
        items.addAll(newItems);
        adapter.addItems(newItems);
        diffResult.dispatchUpdatesTo(adapter);
        if (tempIsOnTop) scrollToTop();
    }

    @Override
    public void onChatStateChanged(Collection<RosterContact> entities) {
        update();
    }

    /**
     * Setup Toolbar scroll behavior according to count of visible chat items
     */
    private void setupToolbarLayout() {
        if (recyclerView != null) {
            int count = items.size();
            if (count <= maxItemsOnScreen) {
                setToolbarScrollEnabled(false);
            } else {
                setToolbarScrollEnabled(true);
            }
        }
    }

    /**
     * Enable or disable Toolbar scroll behavior
     */
    private void setToolbarScrollEnabled(boolean enabled) {
        AppBarLayout.LayoutParams toolbarLayoutParams =
                (AppBarLayout.LayoutParams) toolbarToolbarLayout.getLayoutParams();
        CoordinatorLayout.LayoutParams appBarLayoutParams =
                (CoordinatorLayout.LayoutParams) toolbarAppBarLayout.getLayoutParams();
        if (enabled && toolbarLayoutParams.getScrollFlags() == 0) {
            appBarLayoutParams.setBehavior(new AppBarLayout.Behavior());
            toolbarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        } else if (!enabled && toolbarLayoutParams.getScrollFlags() != 0) {
            toolbarLayoutParams.setScrollFlags(0);
            appBarLayoutParams.setBehavior(null);
        }
        toolbarToolbarLayout.setLayoutParams(toolbarLayoutParams);
        toolbarAppBarLayout.setLayoutParams(appBarLayoutParams);
    }

    @Override
    public void onChatItemSwiped(@NotNull AbstractChat abstractContact) {
        AbstractChat abstractChat = ChatManager.getInstance()
                .getChat(abstractContact.getAccount(), abstractContact.getUser());
        ChatManager.getInstance().getChat(abstractContact.getAccount(), abstractContact.getUser())
                .setArchived(!abstractChat.isArchived());
        showSnackbar(abstractContact, currentChatsState);
        update();
    }

    @Override
    public void onChatAvatarClick(AbstractChat item) {
        Intent intent;
        try {
            intent = ContactViewerActivity.createIntent(getActivity(), item.getAccount(),
                    item.getUser());
            getActivity().startActivity(intent);
        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public void onChatItemContextMenu(ContextMenu menu, AbstractChat contact) {
        try {
            AbstractContact abstractContact = RosterManager.getInstance()
                    .getAbstractContact(contact.getAccount(), contact.getUser());
            ContextMenuHelper.createContactContextMenu(getActivity(), this,
                    abstractContact, menu);
        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void onListBecomeEmpty() {
        if (currentChatsState != ChatListState.recent) currentChatsState = ChatListState.recent;
        update();
    }

    @Override
    public void onChatItemClick(AbstractChat item) {
        try {
            chatListFragmentListener.onChatClick(RosterManager.getInstance()
                    .getAbstractContact(item.getAccount(), item.getUser()));
        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void update() {

        List<AbstractChat> newList = new ArrayList<>();

        /* If filterString is empty, build regular chat list */
        if (filterString == null || filterString.equals("")) {
            if (currentChatsState == ChatListState.recent)
                for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts())
                    if (abstractChat.getLastMessage() != null
                            && !abstractChat.isArchived())
                        newList.add(abstractChat);
            if (currentChatsState == ChatListState.unread)
                for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts())
                    if (abstractChat.getLastMessage() != null
                            && abstractChat.getUnreadMessageCount() != 0)
                        newList.add(abstractChat);
            if (currentChatsState == ChatListState.archived)
                for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts())
                    if (abstractChat.getLastMessage() != null && abstractChat.isArchived())
                        newList.add(abstractChat);

            Collections.sort(newList, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                    o1.getLastTime().getTime()));

        } else {

            ArrayList<AbstractChat> chatsList = new ArrayList<>(
                    getFilteredChatsOfEnabledAccountsByString(ChatManager.getInstance()
                            .getChatsOfEnabledAccounts(), filterString));

            Collections.sort(chatsList, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                    o1.getLastTime().getTime()));

            ArrayList<AbstractChat> contactList = new ArrayList<>(
                    getFilteredContactsOfEnabledAccountsByString(RosterManager.getInstance()
                            .getAllContactsForEnabledAccounts(), filterString));

            newList.clear();
            newList.addAll(concatLists(chatsList, contactList));

        }

        setupMarkAllTheReadButton(newList.size());

        /* Update another elements */
        updateToolbar();
        updateItems(newList);
    }

    private ArrayList<AbstractChat> concatLists(ArrayList<AbstractChat> chatList,
                                                ArrayList<AbstractChat> contactsList) {
        ArrayList<AbstractChat> result = new ArrayList<>(chatList);
        for (AbstractChat abstractChat : contactsList) {
            boolean isDuplicating = false;
            for (AbstractChat abstractChat1 : chatList)
                if (abstractChat.getUser() == abstractChat1.getUser()) {
                    isDuplicating = true;
                    break;
                }
            if (!isDuplicating) result.add(abstractChat);
        }

        return result;
    }

    private void setupMarkAllTheReadButton(int listSize) {
        if (currentChatsState == ChatListState.unread && listSize > 0 && getContext() != null) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light) {
                markAllReadBackground.setColorFilter(ColorManager.getInstance().getAccountPainter()
                        .getDefaultMainColor(), PorterDuff.Mode.SRC_ATOP);
                markAllAsReadButton.setTextColor(getContext().getResources()
                        .getColor(R.color.white));
            } else {
                markAllReadBackground.setColorFilter(getContext().getResources()
                        .getColor(R.color.grey_900), PorterDuff.Mode.SRC_ATOP);
                markAllAsReadButton.setTextColor(ColorManager.getInstance().getAccountPainter()
                        .getDefaultMainColor());
            }

            markAllAsReadButton.setVisibility(View.VISIBLE);
            markAllAsReadButton.setOnClickListener(v -> {
                for (AbstractChat chat : ChatManager.getInstance().getChatsOfEnabledAccounts()) {
                    chat.markAsReadAll(true);
                    MessageNotificationManager.getInstance().removeAllMessageNotifications();
                }
                onStateSelected(ChatListFragment.ChatListState.recent);
                Toast toast = Toast.makeText(getActivity(), R.string.all_chats_were_market_as_read_toast,
                        Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        (int) (getResources().getDimension(R.dimen.bottom_navigation_height) * 1.2f));
                toast.show();
            });
        } else markAllAsReadButton.setVisibility(View.GONE);
    }

    private Collection<AbstractChat> getFilteredChatsOfEnabledAccountsByString(
            Collection<AbstractChat> abstractChats, String filterString) {
        String transliteratedFilterString = StringUtils.translitirateToLatin(filterString);
        Collection<AbstractChat> resultCollection = new ArrayList<>();
        for (AbstractChat abstractChat : abstractChats) {
            AbstractContact abstractContact = RosterManager.getInstance()
                    .getAbstractContact(abstractChat.getAccount(), abstractChat.getUser());
            if (abstractChat.getLastMessage() == null)
                continue;
            if (abstractChat.getUser().toString().contains(filterString)
                    || abstractChat.getUser().toString().contains(transliteratedFilterString)
                    || abstractContact.getName().contains(filterString)
                    || abstractContact.getName().contains(transliteratedFilterString))
                resultCollection.add(abstractChat);
        }
        return resultCollection;
    }

    private Collection<AbstractChat> getFilteredContactsOfEnabledAccountsByString(
            Collection<AbstractContact> abstractContacts, String filterString) {
        String transliteratedFilterString = StringUtils.translitirateToLatin(filterString);
        Collection<AbstractChat> resultCollection = new ArrayList<>();
        for (AbstractContact abstractContact : abstractContacts) {

            if (abstractContact.getUser().toString().contains(filterString)
                    || abstractContact.getUser().toString().contains(transliteratedFilterString)
                    || abstractContact.getName().contains(filterString)
                    || abstractContact.getName().contains(transliteratedFilterString))
                resultCollection.add(new RegularChat(abstractContact.getAccount(),
                        abstractContact.getUser()));
        }
        return resultCollection;
    }

    private void showPlaceholder(String message, @Nullable String buttonMessage) {
        placeholderMessage.setText(message);
        if (buttonMessage != null) {
            placeholderButton.setVisibility(View.VISIBLE);
            placeholderButton.setText(buttonMessage);
        }
        placeholderView.setVisibility(View.VISIBLE);
    }

    private void hidePlaceholder() {
        recyclerView.setVisibility(View.VISIBLE);
        placeholderView.setVisibility(View.GONE);
        placeholderButton.setVisibility(View.GONE);
    }

    private void showSnackbar(final AbstractChat deletedItem, final ChatListState previousState) {
        if (snackbar != null) snackbar.dismiss();
        final AbstractChat abstractChat = ChatManager.getInstance()
                .getChat(deletedItem.getAccount(), deletedItem.getUser());
        final boolean archived = abstractChat.isArchived();
        snackbar = Snackbar.make(coordinatorLayout,
                !archived ? R.string.chat_was_unarchived : R.string.chat_was_archived, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, view -> {
            abstractChat.setArchived(!archived);
            onStateSelected(previousState);
            update();
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
        archived
    }

    public interface ChatListFragmentListener {
        void onChatClick(AbstractContact contact);

        void onChatListStateChanged(ChatListState chatListState);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NOT_SPECIFIED, SHOW_AVATARS, DO_NOT_SHOW_AVATARS})
    public @interface ChatListAvatarState {
    }

}