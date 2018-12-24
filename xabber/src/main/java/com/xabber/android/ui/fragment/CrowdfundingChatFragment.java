package com.xabber.android.ui.fragment;

import android.content.Intent;
import android.net.Uri;
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
import com.xabber.android.data.http.IXabberCom;
import com.xabber.android.ui.adapter.chat.CrowdfundingChatAdapter;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmResults;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class CrowdfundingChatFragment extends Fragment implements CrowdfundingChatAdapter.BindListener {

    private RecyclerView recyclerView;
    private View backgroundView;
    private ImageView ivReload;
    private RealmResults<CrowdfundingMessage> messages = null;
    private LinearLayoutManager layoutManager;
    private CrowdfundingChatAdapter adapter;
    private RelativeLayout btnScrollDown;
    private TextView tvNewReceivedCount;

    private int fakeUnread = 0;
    private int realUnread = 0;
    private List<String> waitToMarkAsRead = new ArrayList<>();
    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    private final int UPDATE_MESSAGE_DELAY = 1; // in sec

    public static CrowdfundingChatFragment newInstance() {
        return new CrowdfundingChatFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        CrowdfundingManager.getInstance().startUpdateTimer(0, UPDATE_MESSAGE_DELAY);
        messages = CrowdfundingManager.getInstance().getMessagesWithDelay(0);
        realUnread = CrowdfundingManager.getInstance().getUnreadMessageCount();
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
                scrollDown();
            }
        });
        tvNewReceivedCount = view.findViewById(R.id.tvNewReceivedCount);
        View actionJoin = view.findViewById(R.id.actionJoin);
        actionJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(IXabberCom.SHARE_URL));
                startActivity(intent);
            }
        });

        return view;
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
        adapter = new CrowdfundingChatAdapter(getActivity(), messages, true, this);

        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateButton();
            }
        });

        scrollToLastPosition();
        subscribeForUnreadCount();
        updateButton();
    }

    @Override
    public void onPause() {
        super.onPause();
        compositeSubscription.clear();
        writeAsRead();
        saveCurrentPosition();
    }

    @Override
    public void onBind(CrowdfundingMessage message) {
        markAsRead(message);
    }

    private void subscribeForUnreadCount() {
        compositeSubscription.add(
            CrowdfundingManager.getInstance().getUnreadMessageCountAsObservable()
                .subscribe(new Action1<RealmResults<CrowdfundingMessage>>() {
                    @Override
                    public void call(RealmResults<CrowdfundingMessage> crowdfundingMessages) {
                        realUnread = crowdfundingMessages.size();
                        updateButton();
                    }
                }));
    }

    private void markAsRead(CrowdfundingMessage message) {
        if (message != null && message.isValid() && !message.isRead()) {
            if (!waitToMarkAsRead.contains(message.getId())) {
                waitToMarkAsRead.add(message.getId());
                updateButton();
            }
        }
    }

    private void markSkippedAsRead(int lastpos) {
        for (int i = 0; i <= lastpos; i++) {
            markAsRead(adapter.getItem(i));
        }
    }

    private void writeAsRead() {
        String[] ids = waitToMarkAsRead.toArray(new String[0]);
        CrowdfundingManager.getInstance().markMessagesAsRead(ids);
    }

    private void updateButton() {
        showScrollDownButtonIfNeed();
        updateNewReceivedMessageCounter();
    }

    private void updateNewReceivedMessageCounter() {
        fakeUnread = realUnread - waitToMarkAsRead.size();

        tvNewReceivedCount.setText(String.valueOf(fakeUnread));
        if (fakeUnread > 0)
            tvNewReceivedCount.setVisibility(View.VISIBLE);
        else tvNewReceivedCount.setVisibility(View.GONE);
    }

    private void showScrollDownButtonIfNeed() {
        int pastVisibleItems = layoutManager.findLastVisibleItemPosition();
        boolean isBottom = pastVisibleItems >= adapter.getItemCount() - 1;

        if (isBottom) {
            btnScrollDown.setVisibility(View.GONE);
        } else btnScrollDown.setVisibility(View.VISIBLE);
    }

    private void saveCurrentPosition() {
        int position = layoutManager.findFirstVisibleItemPosition();
        SettingsManager.setLastCrowdfundingPosition(position);
    }

    private void scrollToLastPosition() {
        int position = SettingsManager.getLastCrowdfundingPosition();
        layoutManager.scrollToPositionWithOffset(position, 0);
    }

    private void scrollDown() {
        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
        if (fakeUnread == 0 || lastVisiblePosition + 2 >= adapter.getItemCount() - fakeUnread) {
            // scroll down
            layoutManager.scrollToPosition(adapter.getItemCount() - 1);
            markSkippedAsRead(adapter.getItemCount() - 1);
            // scroll to unread
        } else layoutManager.scrollToPosition(adapter.getItemCount() - fakeUnread);
    }
}
