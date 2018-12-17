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
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.http.CrowdfundingManager;
import com.xabber.android.ui.adapter.chat.CrowdfundingChatAdapter;
import com.xabber.android.ui.color.ColorManager;

import io.realm.RealmResults;

public class CrowdfundingChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private View backgroundView;
    private ImageView ivReload;
    private RealmResults<CrowdfundingMessage> messages = null;

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
        CrowdfundingChatAdapter adapter = new CrowdfundingChatAdapter(getActivity(), messages, true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // mark messages read
        if (CrowdfundingManager.getInstance().getUnreadMessageCount() > 0) {
            CrowdfundingManager.getInstance().markMessagesAsRead();
        }
    }
}
