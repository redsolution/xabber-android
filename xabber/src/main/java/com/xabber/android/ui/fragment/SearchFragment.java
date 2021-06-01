package com.xabber.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.database.realmobjects.RecentSearchRealmObject;
import com.xabber.android.data.database.repositories.RecentSearchRealmObjectRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.message.chat.RegularChat;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.activity.SearchActivity;
import com.xabber.android.ui.adapter.SearchContactsListItemAdapter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.ui.fragment.chatListFragment.ChatListAdapter;
import com.xabber.android.ui.fragment.chatListFragment.ChatListFragment;
import com.xabber.android.ui.fragment.chatListFragment.ChatListItemListener;
import com.xabber.android.utils.StringUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SearchFragment extends Fragment implements View.OnClickListener,
        OnAccountChangedListener, OnContactChangedListener, ChatListItemListener,
        SearchContactsListItemAdapter.SearchContactsListItemListener {

    private static final String LOG_TAG = SearchFragment.class.getSimpleName();

    private NestedScrollView nestedScrollView;

    /* ContactsList variables */
    private View contactListRoot;

    /* RecentList variables */
    private ConstraintLayout recentListRootConstraintLayout;
    private RelativeLayout recentListTitleRootView;
    private ChatListAdapter recentChatListAdapter;
    private TextView recentPlaceholder;

    /* Search variables */
    private RecyclerView searchListRecyclerView;
    private ChatListAdapter searchChatListAdapter;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onAttach(Context context) {
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        super.onDetach();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        nestedScrollView = view.findViewById(R.id.search_greetings_root);

        searchListRecyclerView = view.findViewById(R.id.search_list);
        searchListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false));
        searchChatListAdapter = new ChatListAdapter(new ArrayList<AbstractChat>(), this, false);
        searchListRecyclerView.setAdapter(searchChatListAdapter);

        initContactsList(view);
        initRecent(view);

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        buildChatsListWithFilter(null);
        update();
    }

    /**
     * OnClickListener for Toolbar
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search_recent_list_title_clear_button) {
            RecentSearchRealmObjectRepository.clearAllRecentSearched();
            buildChatsListWithFilter(null);
        }
    }

    @Override
    public void onContactListItemClick(@NotNull AbstractChat contact) {
        try {

            ((SearchActivity) getActivity()).onChatClick(RosterManager.getInstance()
                    .getAbstractContact(contact.getAccount(), contact.getUser()));

            RecentSearchRealmObjectRepository
                    .itemWasSearched(contact.getAccount(), contact.getUser());

        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void onChatItemClick(@NotNull AbstractChat contact) {
        try {

            ((SearchActivity) getActivity()).onChatClick(RosterManager.getInstance()
                    .getAbstractContact(contact.getAccount(), contact.getUser()));

            RecentSearchRealmObjectRepository
                    .itemWasSearched(contact.getAccount(), contact.getUser());

        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void onChatAvatarClick(@NotNull AbstractChat contact) {
        try {
            ((MainActivity) getActivity()).onChatClick(RosterManager.getInstance()
                    .getAbstractContact(contact.getAccount(), contact.getUser()));
        } catch (Exception e) {
            LogManager.exception(ChatListFragment.class.toString(), e);
        }
    }

    @Override
    public void onChatItemContextMenu(@NotNull ContextMenu menu, @NotNull AbstractChat contact) {
        //todo disable context menu
    }

    @Override
    public void onChatItemSwiped(@NotNull AbstractChat abstractContact) {
        //todo disable swiping
    }

    @Override
    public void onListBecomeEmpty() {
        //todo implement this
    }

    public void update() {
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        update();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        update();
    }

    private void initContactsList(View view) {
        TextView title = view.findViewById(R.id.search_contacts_list_title);
        contactListRoot = view.findViewById(R.id.search_contacts_list_root);
        RecyclerView contactListRecyclerView = view.findViewById(R.id.search_contacts_list_recycler);
        View contactListAccountColorIndicator = view.findViewById(R.id.search_contact_list_account_color_indicator);

        LinearLayoutManager chatListLinearLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.HORIZONTAL, false);
        contactListRecyclerView.setLayoutManager(chatListLinearLayoutManager);

        /* Update left color indicator via current main user */
        if (AccountManager.getInstance().getEnabledAccounts().size() > 1) {
            contactListAccountColorIndicator.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        } else {
            contactListAccountColorIndicator.setBackgroundColor(
                    getResources().getColor(R.color.transparent));
        }

        ArrayList<AbstractChat> onlineChats = new ArrayList<>();
        for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts()) {
            StatusMode statusMode = RosterManager.getInstance()
                    .getAbstractContact(abstractChat.getAccount(), abstractChat.getUser()).getStatusMode();
            if (abstractChat.getLastMessage() != null
                    && !abstractChat.isArchived()
                    && !abstractChat.isGroupchat()
                    && !abstractChat.getUser().getJid().isDomainBareJid()
                    && (statusMode.equals(StatusMode.chat) || statusMode.equals(StatusMode.available)))
                onlineChats.add(abstractChat);
        }

        Collections.sort(onlineChats, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                o1.getLastTime().getTime()));

        ArrayList<AbstractChat> otherChats = new ArrayList<>();
        for (AbstractChat abstractChat : ChatManager.getInstance().getChatsOfEnabledAccounts()) {
            if (abstractChat.getLastMessage() != null
                    && !abstractChat.isArchived()
                    && !abstractChat.isGroupchat()
                    && !abstractChat.getUser().getJid().isDomainBareJid())
                otherChats.add(abstractChat);
        }

        Collections.sort(otherChats, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                o1.getLastTime().getTime()));

        ArrayList<AbstractChat> allChats = concatLists(onlineChats, otherChats);

        if (allChats.size() > 15)
            allChats = new ArrayList<>(allChats.subList(0, 14));

        if (allChats.isEmpty()){
            title.setVisibility(View.GONE);
            contactListAccountColorIndicator.setVisibility(View.GONE);
        } else {
            title.setVisibility(View.VISIBLE);
            contactListAccountColorIndicator.setVisibility(View.VISIBLE);
        }

        SearchContactsListItemAdapter contactsListAdapter = new SearchContactsListItemAdapter(allChats, this);
        contactListRecyclerView.setAdapter(contactsListAdapter);

    }

    private void initRecent(View view) {
        recentListRootConstraintLayout = view.findViewById(R.id.search_recent_root_constraint_layout);
        RecyclerView recentListRecyclerView = view.findViewById(R.id.search_recent_list_recycler);
        TextView recentListClearBtn = view.findViewById(R.id.search_recent_list_title_clear_button);
        View colorIndicator = view.findViewById(R.id.search_recent_list_account_color_indicator);
        recentListTitleRootView = view.findViewById(R.id.search_recent_list_title_root_relative_layout);
        recentPlaceholder = view.findViewById(R.id.search_recent_placeholder);

        recentListClearBtn.setOnClickListener(this);

        /* Update left color indicator via current main user */
        if (AccountManager.getInstance().getEnabledAccounts().size() > 1) {
            colorIndicator.setBackgroundColor(
                    ColorManager.getInstance().getAccountPainter().getDefaultMainColor());
        } else {
            colorIndicator.setBackgroundColor(
                    getResources().getColor(R.color.transparent));
        }

        LinearLayoutManager recentListLinearLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.VERTICAL, false);
        recentListRecyclerView.setLayoutManager(recentListLinearLayoutManager);

        ArrayList<AbstractChat> chats = new ArrayList<>();
        if (!RecentSearchRealmObjectRepository.getAllRecentSearchRealmObjects().isEmpty()) {
            for (RecentSearchRealmObject recentSearchRealmObject :
                    RecentSearchRealmObjectRepository.getAllRecentSearchRealmObjects()) {
                try {
                    AccountJid accountJid = AccountJid
                            .from(recentSearchRealmObject.getAccountJid());

                    ContactJid contactJid = ContactJid
                            .from(recentSearchRealmObject.getContactJid());

                    if (ChatManager.getInstance().hasChat(recentSearchRealmObject.getAccountJid(),
                            recentSearchRealmObject.getContactJid())) {

                        AbstractChat abstractChat = ChatManager.getInstance()
                                .getOrCreateChat(accountJid, contactJid);

                        if (abstractChat.getLastMessage() != null && !abstractChat.isArchived())
                            chats.add(abstractChat);

                    } else chats.add(new RegularChat(accountJid, contactJid));

                } catch (Exception e) {
                    LogManager.exception(LOG_TAG, e);
                }
            }
            recentListRootConstraintLayout.setVisibility(View.VISIBLE);
        } else {
            recentListRootConstraintLayout.setVisibility(View.GONE);
        }

        recentChatListAdapter = new ChatListAdapter(chats, this, false);
        recentListRecyclerView.setAdapter(recentChatListAdapter);

        buildChatsListWithFilter(null);
    }

    public void buildChatsListWithFilter(@Nullable String filterString) {
        if (filterString != null && !filterString.isEmpty()) {
            ArrayList<AbstractChat> chatsList = new ArrayList<>(
                    getFilteredChatsOfEnabledAccountsByString(ChatManager.getInstance()
                            .getChatsOfEnabledAccounts(), filterString));

            Collections.sort(chatsList, (o1, o2) -> Long.compare(o2.getLastTime().getTime(),
                    o1.getLastTime().getTime()));

            ArrayList<AbstractChat> contactList = new ArrayList<>(
                    getFilteredContactsOfEnabledAccountsByString(RosterManager.getInstance()
                            .getAllContactsForEnabledAccounts(), filterString));

            nestedScrollView.setVisibility(View.GONE);
            searchListRecyclerView.setVisibility(View.VISIBLE);

            searchChatListAdapter.clear();
            searchChatListAdapter.addItems(concatLists(chatsList, contactList));
            searchChatListAdapter.notifyDataSetChanged();

            if (concatLists(chatsList, contactList).size() == 0)
                recentPlaceholder.setVisibility(View.VISIBLE);
            else recentPlaceholder.setVisibility(View.GONE);

        } else {
            nestedScrollView.setVisibility(View.VISIBLE);
            searchListRecyclerView.setVisibility(View.GONE);

            recentPlaceholder.setVisibility(View.GONE);
        }

    }

    private Collection<AbstractChat> getFilteredChatsOfEnabledAccountsByString(
            Collection<AbstractChat> abstractChats, String filterString) {
        String transliteratedFilterString = StringUtils.translitirateToLatin(filterString);
        Collection<AbstractChat> resultCollection = new ArrayList<>();
        for (AbstractChat abstractChat : abstractChats) {

            if (abstractChat.getLastMessage() == null)
                continue;

            String contactName = RosterManager.getInstance()
                    .getBestContact(abstractChat.getAccount(), abstractChat.getUser())
                    .getName()
                    .toLowerCase();

            if (abstractChat.getUser().toString().contains(filterString)
                    || abstractChat.getUser().toString().contains(transliteratedFilterString)
                    || contactName.contains(filterString)
                    || contactName.contains(transliteratedFilterString))

                resultCollection.add(abstractChat);

        }
        return resultCollection;
    }

    private Collection<AbstractChat> getFilteredContactsOfEnabledAccountsByString(
            Collection<AbstractContact> abstractContacts, String filterString) {

        String transliteratedFilterString = StringUtils.translitirateToLatin(filterString);
        Collection<AbstractChat> resultCollection = new ArrayList<>();

        for (AbstractContact abstractContact : abstractContacts) {

            String name = RosterManager.getInstance()
                    .getBestContact(abstractContact.getAccount(), abstractContact.getUser())
                    .getName()
                    .toLowerCase();

            if (abstractContact.getUser().toString().contains(filterString)
                    || abstractContact.getUser().toString().contains(transliteratedFilterString)
                    || name.contains(filterString)
                    || name.contains(transliteratedFilterString))

                resultCollection.add(new RegularChat(abstractContact.getAccount(),
                        abstractContact.getUser()));
        }
        return resultCollection;
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

}
