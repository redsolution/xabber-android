package com.xabber.android.ui.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.http.CrowdfundingManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.adapter.chat.CrowdfundingChatAdapter;
import com.xabber.android.ui.color.ColorManager;

import io.realm.RealmResults;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class CrowdfundingChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private View backgroundView;
    private ImageView ivReload;
    private RealmResults<CrowdfundingMessage> messages = null;
    private LinearLayoutManager layoutManager;
    private CrowdfundingChatAdapter adapter;
    private RelativeLayout btnScrollDown;
    private TextView tvNewReceivedCount;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    private final int UPDATE_MESSAGE_DELAY = 5; // in sec

    public static CrowdfundingChatFragment newInstance() {
        return new CrowdfundingChatFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        CrowdfundingManager.getInstance().startUpdateTimer(0, UPDATE_MESSAGE_DELAY);
        messages = CrowdfundingManager.getInstance().getMessagesWithDelay(0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crowdfunding, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        backgroundView = view.findViewById(R.id.backgroundView);
        ivReload = view.findViewById(R.id.ivReload);
        ivReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "RELOAD", Toast.LENGTH_SHORT).show();
                CrowdfundingManager.getInstance().reloadMessages();
            }
        });
        btnScrollDown = view.findViewById(R.id.btnScrollDown);
        btnScrollDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //scrollToFirstUnread(chat.getUnreadMessageCount());
            }
        });
        tvNewReceivedCount = view.findViewById(R.id.tvNewReceivedCount);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        compositeSubscription.clear();
    }

    @Override
    public void onResume() {
        super.onResume();

        // background
        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat_dark);
            } else {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat);
            }
        } else {
            backgroundView.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

        // messages
        adapter = new CrowdfundingChatAdapter(getActivity(), messages, true);

        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                showScrollDownButtonIfNeed();
                hideUnreadMessageCountIfNeed();
            }
        });

        // mark messages read
//        if (CrowdfundingManager.getInstance().getUnreadMessageCount() > 0) {
//            CrowdfundingManager.getInstance().markMessagesAsRead();
//        }

        subscribeForUnreadCount();
        restoreScrollState();
    }

    private void increaseUnreadMessageCountIfNeed(int unread) {
        if (btnScrollDown.getVisibility() == View.VISIBLE) {
            updateNewReceivedMessageCounter(unread);
        }
    }

    private void updateNewReceivedMessageCounter(int count) {
        tvNewReceivedCount.setText(String.valueOf(count));
        if (count > 0)
            tvNewReceivedCount.setVisibility(View.VISIBLE);
        else tvNewReceivedCount.setVisibility(View.GONE);
    }

    private void hideUnreadMessageCountIfNeed() {
        int pastVisibleItems = layoutManager.findLastVisibleItemPosition();
        if (pastVisibleItems >= adapter.getItemCount() - CrowdfundingManager.getInstance().getUnreadMessageCount()) {
            resetUnreadMessageCount();
        }
    }

    private void resetUnreadMessageCount() {
        //CrowdfundingManager.getInstance().markMessagesAsRead();
        ((ChatActivity)getActivity()).updateRecentChats();
    }

    private void showScrollDownButtonIfNeed() {
        int pastVisibleItems = layoutManager.findLastVisibleItemPosition();
        boolean isBottom = pastVisibleItems >= adapter.getItemCount() - 1;

        if (isBottom) {
            btnScrollDown.setVisibility(View.GONE);
            //hideUnreadMessageBackground();
        } else btnScrollDown.setVisibility(View.VISIBLE);
    }

    private void subscribeForUnreadCount() {
        compositeSubscription.add(
            CrowdfundingManager.getInstance().getUnreadMessageCountAsObservable()
                .subscribe(new Action1<RealmResults<CrowdfundingMessage>>() {
                    @Override
                    public void call(RealmResults<CrowdfundingMessage> crowdfundingMessages) {
                        showScrollDownButtonIfNeed();
                        increaseUnreadMessageCountIfNeed(crowdfundingMessages.size());
                    }
                }));
    }

    private void scrollToFirstUnread(int unreadCount) {
        layoutManager.scrollToPositionWithOffset(
                adapter.getItemCount() - unreadCount, 200);
    }

    public void restoreScrollState() {
        int unread = CrowdfundingManager.getInstance().getUnreadMessageCount();
        scrollToFirstUnread(unread);
        updateNewReceivedMessageCounter(unread);
    }
}
