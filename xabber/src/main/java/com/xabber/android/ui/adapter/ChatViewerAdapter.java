package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.ui.fragment.ChatFragment;
import com.xabber.android.ui.fragment.RecentChatFragment;

public class ChatViewerAdapter extends FragmentPagerAdapter {

    private static final String LOG_TAG = ChatViewerAdapter.class.getSimpleName();
    public static final int PAGE_POSITION_RECENT_CHATS = 0;
    public static final int PAGE_POSITION_CHAT = 1;

    private final FragmentManager fragmentManager;
    @Nullable
    private RecentChatFragment recentChatFragment;
    @Nullable
    private ChatFragment chatFragment;

    private AccountJid accountJid;
    private UserJid userJid;

    private int itemCount;

    public ChatViewerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        itemCount = 1;
    }

    public ChatViewerAdapter(FragmentManager fragmentManager, @NonNull AccountJid accountJid,
                             @NonNull UserJid userJid) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        setChat(accountJid, userJid);
    }

    public void selectChat(@NonNull AccountJid accountJid, @NonNull UserJid userJid) {
        if (accountJid.equals(this.accountJid) && userJid.equals(this.userJid)) {
            return;
        }

        setChat(accountJid, userJid);

        if (chatFragment != null) {
            fragmentManager.beginTransaction().remove(chatFragment).commit();
            chatFragment = null;
        }

        notifyDataSetChanged();
    }

    private void setChat(@NonNull AccountJid accountJid, @NonNull UserJid userJid) {
        itemCount = 2;
        this.accountJid = accountJid;
        this.userJid = userJid;
    }

    @Override
    public Fragment getItem(int position) {
        if (position != PAGE_POSITION_RECENT_CHATS) {
            return getOrCreateChatFragment();
        } else {
            return getOrCreateRecentChatFragment();
        }
    }

    @Override
    public int getCount() {
        return itemCount;
    }

    @Override
    public int getItemPosition(Object object) {
        if (object instanceof ChatFragment) {
            return POSITION_NONE;
        }

        return POSITION_UNCHANGED;
    }

    private RecentChatFragment getOrCreateRecentChatFragment() {
        if (recentChatFragment == null) {
            recentChatFragment = RecentChatFragment.newInstance();
        }

        return recentChatFragment;
    }

    private ChatFragment getOrCreateChatFragment() {
        if (chatFragment == null) {
            chatFragment = ChatFragment.newInstance(accountJid, userJid);
        }

        return chatFragment;
    }

    @Nullable
    public RecentChatFragment getRecentChatFragment() {
        return recentChatFragment;
    }

    @Nullable
    public ChatFragment getChatFragment() {
        return chatFragment;
    }
}