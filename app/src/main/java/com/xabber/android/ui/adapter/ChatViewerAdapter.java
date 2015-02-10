package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.android.ui.RecentChatFragment;
import com.xabber.xmpp.address.Jid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static java.lang.Math.abs;

public class ChatViewerAdapter extends FragmentStatePagerAdapter implements UpdatableAdapter {

    /**
     * Intent sent while opening chat activity.
     */
    private final AbstractChat intent;

    private ArrayList<AbstractChat> activeChats;

    private FinishUpdateListener finishUpdateListener;

    private Fragment currentFragment;

    private static final int TOTAL_COUNT = 200;
    private static final int OFFSET = TOTAL_COUNT / 2;


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

    public ChatViewerAdapter(FragmentManager fragmentManager, FinishUpdateListener finishUpdateListener) {
        super(fragmentManager);
        this.finishUpdateListener = finishUpdateListener;

        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());
        intent = null;
        onChange();
    }

    @Override
    public int getCount() {
        // warning: scrolling to very high values (1,000,000+) results in
        // strange drawing behaviour
        return TOTAL_COUNT;
    }

    public int getRealCount() {
        return activeChats.size() + 1;
    }

    @Override
    public Fragment getItem(int virtualPagePosition) {
        int realPosition = getRealPagePosition(virtualPagePosition);

        if (realPosition == 0) {
            RecentChatFragment activeChatFragment = RecentChatFragment.newInstance();
            activeChatFragment.setInitialChats(activeChats);
            return activeChatFragment;
        }

        AbstractChat chat = activeChats.get(getChatIndexFromRealPosition(realPosition));
        return ChatViewerFragment.newInstance(chat.getAccount(), chat.getUser());
    }

    @Override
    public void onChange() {
        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());

        if (intent != null && !activeChats.contains(intent)) {
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
        int realPosition = 0;

        for (int chatIndex = 0; chatIndex < activeChats.size(); chatIndex++) {
            if (activeChats.get(chatIndex).equals(account, user)) {
                realPosition = chatIndex + 1;
                break;
            }
        }

        return realPosition + OFFSET;
    }

    public int getRecentChatsPosition() {
        return OFFSET;
    }

    public AbstractChat getChatByPageNumber(int virtualPosition) {
        int realPosition = getRealPagePosition(virtualPosition);

        if (realPosition == 0) {
            return null;
        }
        return activeChats.get(getChatIndexFromRealPosition(realPosition));
    }


    private int getRealPagePosition(int virtualPosition) {
        return abs(virtualPosition - OFFSET) % getRealCount();
    }

    private int getChatIndexFromRealPosition(int virtualPosition) {
        return virtualPosition - 1;
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

    public ArrayList<AbstractChat> getActiveChats() {
        return activeChats;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}