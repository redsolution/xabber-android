package com.xabber.android.presentation.ui.contactlist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomChat;
import com.xabber.android.data.extension.muc.RoomContact;
import com.xabber.android.data.http.CrowdfundingManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.ChatContact;
import com.xabber.android.data.message.CrowdfundingChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.android.data.message.NewMessageEvent;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.CrowdfundingContact;
import com.xabber.android.data.roster.GroupManager;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.presentation.mvp.contactlist.ContactListPresenter;
import com.xabber.android.presentation.mvp.contactlist.UpdateBackpressure;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ChatVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.CrowdfundingChatVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.GroupVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ToolbarVO;
import com.xabber.android.ui.activity.ConferenceSelectActivity;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactAddActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.contactlist.AccountConfiguration;
import com.xabber.android.ui.adapter.contactlist.ContactListGroupUtils;
import com.xabber.android.ui.adapter.contactlist.GroupConfiguration;
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
import java.util.Map;
import java.util.TreeMap;
import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class ChatListFragment extends Fragment implements ContactVO.ContactClickListener,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemSwipeListener,
        OnContactChangedListener, OnAccountChangedListener, ToolbarVO.OnClickListener,
        UpdateBackpressure.UpdatableObject {

    private UpdateBackpressure updateBackpressure;
    private FlexibleAdapter<IFlexible> adapter;
    private List<IFlexible> items;
    private Snackbar snackbar;
    private CoordinatorLayout coordinatorLayout;
    private LinearLayoutManager linearLayoutManager;
    private View placeholderView;
    private TextView placeholderMessage;
    private ImageView placeholderImage;
    private ChatListFragmentListener chatListFragmentListener;
    private ChatListState currentChatsState = ChatListState.recent;
    private RecyclerView recyclerView;

    public interface ChatListFragmentListener{
        void onChatClick(AbstractContact contact);
        void onChatListStateChanged(ChatListState chatListState);
    }

    @Override
    public void onAttach(Context context) {
        chatListFragmentListener = (ChatListFragmentListener) context;
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        EventBus.getDefault().register(this);
        super.onAttach(context);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(NewMessageEvent event) {
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

    public static ChatListFragment newInstance(@Nullable AccountJid account){
        ChatListFragment fragment = new ChatListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable("account_jid", account);
        fragment.setArguments(args);
        return fragment;
    }

    public void showChatListWithState(ChatListState state){
        currentChatsState = state;
        updateBackpressure.run();
    }

    @Override
    public void onStateSelected(ChatListState state) {
        this.currentChatsState = state;
        updateBackpressure.run();
        chatListFragmentListener.onChatListStateChanged(state);
        this.closeSnackbar();
    }

    public ChatListState getCurrentChatsState(){
        return currentChatsState;
    }

    @Override
    public void onAddContactClick() {
        startActivity(ContactAddActivity.createIntent(getActivity()));
    }

    @Override
    public void onJoinConferenceClick() {
        startActivity(ConferenceSelectActivity.createIntent(getActivity()));
    }

    @Override
    public void onSetStatusClick() {
        startActivity(StatusEditActivity.createIntent(getActivity()));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO implement scroll to account if it need;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.chatlist_recyclerview);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.chatlist_coordinator_layout);
        placeholderView = view.findViewById(R.id.chatlist_placeholder_view);
        placeholderMessage = view.findViewById(R.id.chatlist_placeholder_message);
        placeholderImage = view.findViewById(R.id.chatlist_placeholder_image);
        ColorManager.setGrayScaleFilter(placeholderImage);

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, null, false);
        recyclerView.setAdapter(adapter);
        adapter.setDisplayHeadersAtStartUp(true);
        adapter.setSwipeEnabled(true);
        adapter.expandItemsAtStartUp();
        adapter.setStickyHeaders(true);

        adapter.addListener(this);
        updateBackpressure = new UpdateBackpressure(this);
        updateBackpressure.run();
        updateBackpressure.refreshRequest();
        return view;
    }



    // notify adapter to update ui items
    private void updateItems(List<IFlexible> items){
        //check empty state and show placeholder
        if (items.size() <= 1){
            if (currentChatsState == ChatListState.unread) showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.placeholder_no_unread));
            else if (currentChatsState == ChatListState.archived) showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.placeholder_no_archived));
        } else if (AccountManager.getInstance().getCommonState() != CommonState.online){
            showPlaceholder(Application.getInstance().getApplicationContext().getString(R.string.application_state_waiting));
            recyclerView.setVisibility(View.GONE);
        } else hidePlaceholder();

        this.items.clear();
        this.items.addAll(items);
        adapter.updateDataSet(this.items);
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        updateBackpressure.refreshRequest();
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

    public void updateUnreadCount() {
        int unreadMessageCount = 0;

        for (AbstractChat abstractChat : MessageManager.getInstance().getChatsOfEnabledAccount()) {
            if (abstractChat.notifyAboutMessage() && !abstractChat.isArchived())
                unreadMessageCount += abstractChat.getUnreadMessageCount();
        }

        unreadMessageCount += CrowdfundingManager.getInstance().getUnreadMessageCount();
        EventBus.getDefault().post(new ContactListPresenter.UpdateUnreadCountEvent(unreadMessageCount));
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
        else if (item instanceof CrowdfundingChatVO) {
            AccountJid accountJid = CrowdfundingChat.getDefaultAccount();
            UserJid userJid = CrowdfundingChat.getDefaultUser();
            if (accountJid != null && userJid != null)
                chatListFragmentListener.onChatClick(RosterManager.getInstance().getAbstractContact(accountJid, userJid));
        }

        return true;
    }

    @Override
    public void update(){
        List<IFlexible> items = new ArrayList<>();

        final Collection<RosterContact> allRosterContacts = RosterManager.getInstance().getAllContacts(); // Получаем все контакты
        Map<AccountJid, Collection<UserJid>> blockedContacts = new TreeMap<>();
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            blockedContacts.put(account, BlockingManager.getInstance().getCachedBlockedContacts(account));
        } // получаем map с заблокированными контактами

        final Collection<RosterContact> rosterContacts = new ArrayList<>();
        for (RosterContact contact : allRosterContacts) {
            if (blockedContacts.containsKey(contact.getAccount())) {
                Collection<UserJid> blockedUsers = blockedContacts.get(contact.getAccount());
                if (blockedUsers != null) {
                    if (!blockedUsers.contains(contact.getUser()))
                        rosterContacts.add(contact);
                } else rosterContacts.add(contact);
            } else rosterContacts.add(contact);
        } // фильтруем среди всех контактов убираем заблокированные

        final boolean showOffline = SettingsManager.contactsShowOffline();
        final boolean showGroups = SettingsManager.contactsShowGroups();
        final boolean showEmptyGroups = SettingsManager.contactsShowEmptyGroups();
        final boolean showActiveChats = false;
        final boolean stayActiveChats = true;
        final boolean showAccounts = SettingsManager.contactsShowAccounts();
        boolean hasContacts = false;
        boolean hasVisibleContacts = false;

        final Comparator<AbstractContact> comparator = SettingsManager.contactsOrder();
        final CommonState commonState = AccountManager.getInstance().getCommonState();
        final AccountJid selectedAccount = AccountManager.getInstance().getSelectedAccount();
        final Map<String, GroupConfiguration> groups;
        final List<AbstractContact> contacts;
        final GroupConfiguration chatsGroup;
        final Map<AccountJid, AccountConfiguration> accounts = new TreeMap<>();
        for (AccountJid account : AccountManager.getInstance().getEnabledAccounts()) {
            accounts.put(account, null);
        }
        //List of rooms and active chats grouped by users inside accounts.
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

        // BUILD STRUCTURE //

        // Create arrays.
        if (showAccounts) {
            groups = null;
            contacts = null;
            for (Map.Entry<AccountJid, AccountConfiguration> entry : accounts.entrySet()) {
                entry.setValue(new AccountConfiguration(entry.getKey(),
                        GroupManager.IS_ACCOUNT, GroupManager.getInstance()));
            }
        } else {
            if (showGroups) {
                groups = new TreeMap<>();
                contacts = null;
            } else {
                groups = null;
                contacts = new ArrayList<>();
            }
        }

        // chats on top
        Collection<AbstractChat> chats = MessageManager.getInstance().getChatsOfEnabledAccount();
        chatsGroup = getChatsGroup(chats, currentChatsState);
        if (!chatsGroup.isEmpty()) hasVisibleContacts = true;

        // Build structure.
        for (RosterContact rosterContact : rosterContacts) {
            if (!rosterContact.isEnabled()) {
                continue;
            }
            hasContacts = true;
            final boolean online = rosterContact.getStatusMode().isOnline();
            final AccountJid account = rosterContact.getAccount();
            final Map<UserJid, AbstractChat> users = abstractChats.get(account);
            final AbstractChat abstractChat;
            if (users == null) {
                abstractChat = null;
            } else {
                abstractChat = users.remove(rosterContact.getUser());
            }

            if (selectedAccount != null && !selectedAccount.equals(account)) {
                continue;
            }
            if (ContactListGroupUtils.addContact(rosterContact, online, accounts, groups,
                    contacts, showAccounts, showGroups, showOffline)) {
                hasVisibleContacts = true;
            }
        }
        for (Map<UserJid, AbstractChat> users : abstractChats.values())
            for (AbstractChat abstractChat : users.values()) {
                final AbstractContact abstractContact;
                if (abstractChat instanceof RoomChat) {
                    abstractContact = new RoomContact((RoomChat) abstractChat);
                } else {
                    abstractContact = new ChatContact(abstractChat);
                }
                if (selectedAccount != null && !selectedAccount.equals(abstractChat.getAccount())) {
                    continue;
                }
                final String group;
                final boolean online;
                if (abstractChat instanceof RoomChat) {
                    group = GroupManager.IS_ROOM;
                    online = abstractContact.getStatusMode().isOnline();
                } else if (MUCManager.getInstance().isMucPrivateChat(abstractChat.getAccount(), abstractChat.getUser())) {
                    group = GroupManager.IS_ROOM;
                    online = abstractContact.getStatusMode().isOnline();
                } else {
                    group = GroupManager.NO_GROUP;
                    online = false;
                }
                hasVisibleContacts = true;
                ContactListGroupUtils.addContact(abstractContact, group, online, accounts, groups, contacts,
                        showAccounts, showGroups);
            }

        // BUILD STRUCTURE //

        // Remove empty groups, sort and apply structure.
        items.clear();

        /** adding toolbar with avatar of main(top) user account) */
        ArrayList<AccountConfiguration> accountsConfigurationList = new ArrayList<AccountConfiguration>(accounts.values());
        if (accountsConfigurationList.size() != 0){
            AccountJid mainAccountJid = accountsConfigurationList.get(0).getAccount();
            AccountItem mainAccountItem = AccountManager.getInstance().getAccount(mainAccountJid);
            Drawable mainAccountAvatar = AvatarManager.getInstance().getAccountAvatar(mainAccountJid);
            int mainAccountStatusMode = mainAccountItem.getDisplayStatusMode().getStatusLevel();
            items.add(new ToolbarVO(Application.getInstance().getApplicationContext(),
                    this, currentChatsState, mainAccountAvatar,mainAccountStatusMode));
        }

        /** adding crowdfunding chat item */
        CrowdfundingMessage message = CrowdfundingManager.getInstance().getLastNotDelayedMessageFromRealm();
        if (message != null) hasVisibleContacts = true;

        if (hasVisibleContacts) {
            if (currentChatsState == ChatListState.recent){
                int i = 0;
                for (AbstractContact contact : chatsGroup.getAbstractContacts()) {
                    if (contact instanceof CrowdfundingContact) {
                        items.add(CrowdfundingChatVO.convert((CrowdfundingContact) contact));
                    }
                    else items.add(ChatVO.convert(contact, this, null));
                    i++;
                }
            } else {
                for (AbstractContact contact : chatsGroup.getAbstractContacts()) {
                    if (contact instanceof CrowdfundingContact)
                        items.add(CrowdfundingChatVO.convert((CrowdfundingContact) contact));
                    else items.add(ChatVO.convert(contact, this, null));
                }
            }
        }

        updateUnreadCount();
        updateItems(items);

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


        // crowdfunding chat
        int unreadCount = CrowdfundingManager.getInstance().getUnreadMessageCount();
        CrowdfundingMessage message = CrowdfundingManager.getInstance().getLastNotDelayedMessageFromRealm();
        if (message != null) {
            switch (state) {
                case unread:
                    if (unreadCount > 0) newChats.add(CrowdfundingChat.createCrowdfundingChat(unreadCount, message));
                    break;
                case archived:
                    break;
                default:
                    // recent
                    newChats.add(CrowdfundingChat.createCrowdfundingChat(unreadCount, message));
                    break;
            }
        }

        Collections.sort(newChats, ChatComparator.CHAT_COMPARATOR);
        chatsGroup.setNotEmpty();

        int itemsCount = 0;
        for (AbstractChat chat : newChats) {
            if (itemsCount < 50 || state != ChatListState.recent) {
                if (chat instanceof CrowdfundingChat)
                    chatsGroup.addAbstractContact(new CrowdfundingContact((CrowdfundingChat) chat));
                else chatsGroup.addAbstractContact(RosterManager.getInstance()
                        .getBestContact(chat.getAccount(), chat.getUser()));
                chatsGroup.increment(true);
                itemsCount++;
            } else break;
        }

        ShortcutBuilder.updateShortcuts(Application.getInstance(),
                new ArrayList<>(chatsGroup.getAbstractContacts()));

        return chatsGroup;
    }

    public enum ChatListState {
        recent,
        unread,
        archived,
        all
    }
    private void showPlaceholder(String message){
        placeholderMessage.setText(message);
        placeholderView.setVisibility(View.VISIBLE);
    }

    private void hidePlaceholder(){
        recyclerView.setVisibility(View.VISIBLE);
        placeholderView.setVisibility(View.GONE);
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