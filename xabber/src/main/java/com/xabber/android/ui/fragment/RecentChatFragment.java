package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.database.realm.MessageItem;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.adapter.ChatComparator;
import com.xabber.android.ui.adapter.contactlist.ChatListAdapter;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RecentChatFragment extends Fragment implements ChatListAdapter.Listener {

    private ChatListAdapter adapter;
    @Nullable
    private Listener listener;

    public interface Listener {
        void onChatSelected(BaseEntity entity);
        void registerRecentChatFragment(RecentChatFragment recentChatFragment);
        void unregisterRecentChatFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecentChatFragment() {
    }

    public static RecentChatFragment newInstance() {
        return  new RecentChatFragment();
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

        adapter = new ChatListAdapter(getActivity(), this);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        updateChats();

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_default);
        toolbar.setTitle(R.string.recent_chats);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(getActivity());
            }
        });

        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getDefaultMainColor());

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

    public void updateChats() {
        Thread thread = new Thread("Recent chats") {
            @Override
            public void run() {
                Collection<AbstractChat> chats = MessageManager.getInstance().getChats();

                List<AbstractChat> recentChats = new ArrayList<>();

                for (AbstractChat abstractChat : chats) {
                    MessageItem lastMessage = abstractChat.getLastMessage();

                    if (lastMessage != null && !TextUtils.isEmpty(lastMessage.getText())) {
                        recentChats.add(abstractChat);
                    }
                }

                Collections.sort(recentChats, ChatComparator.CHAT_COMPARATOR);


                final List<AbstractContact> newContacts = new ArrayList<>();

                for (AbstractChat chat : recentChats) {
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
        };
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @Override
    public void onRecentChatClick(AbstractContact contact) {
        if (listener != null) {
            listener.onChatSelected(contact);
        }
    }
}
