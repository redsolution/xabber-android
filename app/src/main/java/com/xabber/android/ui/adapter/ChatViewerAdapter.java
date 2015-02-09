package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.antonyt.infiniteviewpager.InfiniteViewPager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.android.ui.RecentChatFragment;
import com.xabber.xmpp.address.Jid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChatViewerAdapter extends FragmentStatePagerAdapter implements UpdatableAdapter {

    /**
     * Intent sent while opening chat activity.
     */
    private final AbstractChat intent;

    private ArrayList<AbstractChat> activeChats;

    private FinishUpdateListener finishUpdateListener;

    private Fragment currentFragment;

    public ChatViewerAdapter(FragmentManager fragmentManager, String account, String user, FinishUpdateListener finishUpdateListener) {
        super(fragmentManager);
        this.finishUpdateListener = finishUpdateListener;

        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());
        intent = MessageManager.getInstance().getOrCreateChat(account, Jid.getBareAddress(user));

        if (!activeChats.contains(intent)) {
            intent.updateCreationTime();
        }
        onChange();
    }

    @Override
    public int getCount() {
        // warning: scrolling to very high values (1,000,000+) results in
        // strange drawing behaviour
        return InfiniteViewPager.TOTAL_COUNT;
    }

    public int getRealCount() {
        int realCount = activeChats.size();

        return realCount + 1;
    }

    @Override
    public Fragment getItem(int virtualPagePosition) {

        int chatIndex;
        int realPosition = virtualPagePosition % getRealCount();

        if (realPosition == 0) {
            RecentChatFragment activeChatFragment = RecentChatFragment.newInstance();
            activeChatFragment.setInitialChats(activeChats);
            return activeChatFragment;
        } else {
            chatIndex = realPosition -1;
        }

        AbstractChat chat = activeChats.get(chatIndex);
        return ChatViewerFragment.newInstance(chat.getAccount(), chat.getUser());
    }

    @Override
    public void onChange() {
        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());

        if (!activeChats.contains(intent)) {
            activeChats.add(intent);
        }

        Collections.sort(activeChats, new Comparator<AbstractChat>() {
            @Override
            public int compare(AbstractChat lhs, AbstractChat rhs) {
                return lhs.getCreationTime().compareTo(rhs.getCreationTime());
            }
        });

        notifyDataSetChanged();
    }

    public int getPageNumber(String account, String user) {
        for (int position = 0; position < activeChats.size(); position++) {
            if (activeChats.get(position).equals(account, user)) {
                return position + 1;
            }
        }

        return -1;
    }

    public AbstractChat getChatByPageNumber(int virtualPosition) {
        int realPosition = virtualPosition % getRealCount();

        int chatIndex = realPosition - 1;

        if (chatIndex < 0) {
            return null;
        }
        return activeChats.get(chatIndex);
    }

    @Override
    public void startUpdate(ViewGroup container) {
        if (currentFragment instanceof ChatViewerFragment) {
            ((ChatViewerFragment)currentFragment).saveInputState();
        }

        super.startUpdate(container);
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);

        finishUpdateListener.onChatViewAdapterFinishUpdate();

        if (currentFragment instanceof ChatViewerFragment) {
            ((ChatViewerFragment)currentFragment).updateChat(false);
            ((ChatViewerFragment)currentFragment).setInputFocus();
        }
    }

    public interface FinishUpdateListener {
        public void onChatViewAdapterFinishUpdate();
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (getCurrentFragment() != object) {
            currentFragment = ((Fragment) object);
        }

        super.setPrimaryItem(container, position, object);
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }
}