package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.http.CrowdfundingManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.CrowdfundingChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.CrowdfundingContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ChatVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ContactVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.CrowdfundingChatVO;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactEditActivity;
import com.xabber.android.ui.adapter.ChatComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

public class RecentChatFragment extends Fragment implements Toolbar.OnMenuItemClickListener,
        ContactVO.ContactClickListener, FlexibleAdapter.OnItemClickListener,
        ChatVO.IsCurrentChatListener, FlexibleAdapter.OnItemSwipeListener {

    private FlexibleAdapter<IFlexible> adapter;
    private List<IFlexible> items;
    CoordinatorLayout coordinatorLayout;
    Snackbar snackbar;

    @Nullable
    private Listener listener;

    public interface Listener {
        void onChatSelected(AccountJid accountJid, UserJid userJid);
        void registerRecentChatFragment(RecentChatFragment recentChatFragment);
        void unregisterRecentChatFragment();
        boolean isCurrentChat(String account, String user);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecentChatFragment() {
    }

    public static RecentChatFragment newInstance() {
        RecentChatFragment fragment = new RecentChatFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) {
            listener = (Listener) activity;
            listener.registerRecentChatFragment(this);
        }
        else throw new RuntimeException(activity.toString()
                + " must implement RecentChatFragment.Listener");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_recent_chats, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recent_chats_recycler_view);
        coordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinatorLayout);

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, null, false);
        recyclerView.setAdapter(adapter);
        adapter.setSwipeEnabled(true);
        adapter.addListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        updateChats();
        return rootView;
    }

    @Override
    public void onDetach() {
        if (listener != null) {
            listener.unregisterRecentChatFragment();
            listener = null;
        }
        super.onDetach();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
//        if (item.getItemId() == R.id.action_close_chats) {
//            MessageManager.closeActiveChats();
//            updateChats();
//        }

        return false;
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
    public void onContactCreateContextMenu(int adapterPosition, ContextMenu menu) {}

    @Override
    public void onContactButtonClick(int adapterPosition) {}

    public void updateItems(List<AbstractContact> items) {
        this.items.clear();

        for (AbstractContact contact : items) {
            if (contact instanceof CrowdfundingContact)
                this.items.add(CrowdfundingChatVO.convert((CrowdfundingContact) contact));
            else this.items.add(ChatVO.convert(contact, this, this));
        }
        adapter.updateDataSet(this.items);
    }

    public void updateChats() {

        Collection<AbstractChat> chats = MessageManager.getInstance().getChats();

        List<AbstractChat> recentChats = new ArrayList<>();

        for (AbstractChat abstractChat : chats) {
            MessageItem lastMessage = abstractChat.getLastMessage();

            if (lastMessage != null) {
                AccountItem accountItem = AccountManager.getInstance().getAccount(abstractChat.getAccount());
                if (accountItem != null && accountItem.isEnabled()) {
                    recentChats.add(abstractChat);
                }
            }
        }

        // crowdfunding chat
        int unreadCount = CrowdfundingManager.getInstance().getUnreadMessageCount();
        CrowdfundingMessage message = CrowdfundingManager.getInstance().getLastNotDelayedMessageFromRealm();
        if (message != null) recentChats.add(CrowdfundingChat.createCrowdfundingChat(unreadCount, message));

        Collections.sort(recentChats, ChatComparator.CHAT_COMPARATOR);
        final List<AbstractContact> newContacts = new ArrayList<>();

        for (AbstractChat chat : recentChats) {
            if (chat instanceof CrowdfundingChat)
                newContacts.add(new CrowdfundingContact((CrowdfundingChat) chat));
            else if (!chat.isArchived() || ((ChatActivity)getActivity()).isShowArchived())
                newContacts.add(RosterManager.getInstance()
                        .getBestContact(chat.getAccount(), chat.getUser()));
        }

        updateItems(newContacts);
    }

    @Override
    public boolean onItemClick(int position) {

        if (adapter.getItem(position) instanceof ChatVO) {
            ChatVO chat = (ChatVO) adapter.getItem(position);
            if (listener != null && chat != null)
                listener.onChatSelected(chat.getAccountJid(), chat.getUserJid());
        } else if (adapter.getItem(position) instanceof CrowdfundingChatVO) {
            if (listener != null) {
                AccountJid accountJid = CrowdfundingChat.getDefaultAccount();
                UserJid userJid = CrowdfundingChat.getDefaultUser();
                if (accountJid != null && userJid != null)
                    listener.onChatSelected(accountJid, userJid);
            }
        }
        return true;
    }

    @Override
    public void onItemSwipe(int position, int direction) {
        Object itemAtPosition = adapter.getItem(position);
        if (itemAtPosition != null && itemAtPosition instanceof ChatVO) {

            // backup of removed item for undo purpose
            final ChatVO deletedItem = (ChatVO) itemAtPosition;

            // update value
            setChatArchived(deletedItem, !(deletedItem).isArchived());
            deletedItem.setArchived(!(deletedItem).isArchived());


            // remove the item from recycler view
            adapter.removeItem(position);
            if (((ChatActivity)getActivity()).isShowArchived()) adapter.addItem(position, deletedItem);

            // showing snackbar with Undo option
            showSnackbar(deletedItem, position);
        }
    }

    @Override
    public void onActionStateChanged(RecyclerView.ViewHolder viewHolder, int actionState) {}

    @Override
    public boolean isCurrentChat(String account, String user) {
        return listener != null && listener.isCurrentChat(account, user);
    }

    public void showSnackbar(final ChatVO deletedItem, final int deletedIndex) {
        if (snackbar != null) snackbar.dismiss();
        final boolean archived = deletedItem.isArchived();
        snackbar = Snackbar.make(coordinatorLayout, !archived ? R.string.chat_was_unarchived
                : R.string.chat_was_archived, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // update value
                setChatArchived(deletedItem, !archived);
                deletedItem.setArchived(!archived);

                // update item in recycler view
                if (((ChatActivity)getActivity()).isShowArchived())
                    adapter.removeItem(deletedIndex);
                adapter.addItem(deletedIndex, deletedItem);
            }
        });
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    public void closeSnackbar() {
        if (snackbar != null) snackbar.dismiss();
    }

    public void setChatArchived(ChatVO chatVO, boolean archived) {
        AbstractChat chat = MessageManager.getInstance().getChat(chatVO.getAccountJid(), chatVO.getUserJid());
        if (chat != null) chat.setArchived(archived, true);
    }
}
