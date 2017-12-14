package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.contactlist.ChatListAdapter;
import com.xabber.android.ui.adapter.contactlist.RosterChatViewHolder;
import com.xabber.android.ui.adapter.contactlist.viewobjects.BaseRosterItemVO;
import com.xabber.android.ui.adapter.contactlist.viewobjects.ChatVO;
import com.xabber.android.ui.helper.RecyclerItemTouchHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RecentChatFragment extends Fragment implements ChatListAdapter.Listener,
        Toolbar.OnMenuItemClickListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    ChatListAdapter adapter;
    CoordinatorLayout coordinatorLayout;
    Snackbar snackbar;

    @Nullable
    private Listener listener;

    public interface Listener {
        void onChatSelected(BaseEntity entity);
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

        listener = (Listener) activity;
        listener.registerRecentChatFragment(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_recent_chats, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.recent_chats_recycler_view);
        coordinatorLayout = (CoordinatorLayout) rootView.findViewById(R.id.coordinatorLayout);

        adapter = new ChatListAdapter(getActivity(), this);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
                new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

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
        if (item.getItemId() == R.id.action_close_chats) {
            MessageManager.closeActiveChats();
            updateChats();
        }

        return false;
    }

    public void updateChats() {

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                Collection<AbstractChat> chats = MessageManager.getInstance().getChats();

                List<AbstractChat> recentChats = new ArrayList<>();

                for (AbstractChat abstractChat : chats) {
                    MessageItem lastMessage = abstractChat.getLastMessage();

                    if (lastMessage != null && !TextUtils.isEmpty(lastMessage.getText())) {
                        AccountItem accountItem = AccountManager.getInstance().getAccount(abstractChat.getAccount());
                        if (accountItem != null && accountItem.isEnabled()) {
                            recentChats.add(abstractChat);
                        }
                    }
                }

                Collections.sort(recentChats, ChatComparator.CHAT_COMPARATOR);


                final List<AbstractContact> newContacts = new ArrayList<>();

                for (AbstractChat chat : recentChats) {
                    if (!chat.isArchived() || ((ChatActivity)getActivity()).isShowArchived())
                        newContacts.add(RosterManager.getInstance()
                                .getBestContact(chat.getAccount(), chat.getUser()));
                }

                Application.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.updateContacts(newContacts);
                    }
                });
            }
        });
    }

    @Override
    public void onRecentChatClick(AbstractContact contact) {
        if (listener != null) {
            listener.onChatSelected(contact);
        }
    }

    @Override
    public boolean isCurrentChat(String account, String user) {
        return listener != null && listener.isCurrentChat(account, user);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof RosterChatViewHolder) {

            Object itemAtPosition = adapter.getItem(position);
            if (itemAtPosition != null && itemAtPosition instanceof ChatVO) {

                // backup of removed item for undo purpose
                final BaseRosterItemVO deletedItem = (BaseRosterItemVO) itemAtPosition;
                final int deletedIndex = viewHolder.getAdapterPosition();

                // update value
                boolean archived = ((ChatVO) deletedItem).isArchived();
                setChatArchived((ChatVO) deletedItem, !archived);
                ((ChatVO) deletedItem).setArchived(!archived);

                // update item in recycler view
                adapter.removeItem(viewHolder.getAdapterPosition());
                if (((ChatActivity)getActivity()).isShowArchived()) adapter.restoreItem(deletedItem, deletedIndex);

                // showing snackbar with Undo option
                showSnackbar(deletedItem, deletedIndex);
            }
        }
    }

    public void showSnackbar(final BaseRosterItemVO deletedItem, final int deletedIndex) {
        if (snackbar != null) snackbar.dismiss();
        final boolean archived = ((ChatVO) deletedItem).isArchived();
        snackbar = Snackbar.make(coordinatorLayout, !archived ? R.string.chat_was_unarchived
                : R.string.chat_was_archived, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // update value
                setChatArchived((ChatVO) deletedItem, !archived);
                ((ChatVO) deletedItem).setArchived(!archived);

                // update item in recycler view
                if (((ChatActivity)getActivity()).isShowArchived()) adapter.removeItem(deletedIndex);
                adapter.restoreItem(deletedItem, deletedIndex);
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
