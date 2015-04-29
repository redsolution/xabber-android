package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.android.ui.RecentChatFragment;
import com.xabber.xmpp.address.Jid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static java.lang.Math.abs;

public class ChatViewerAdapter extends FragmentStatePagerAdapter {

    /**
     * Intent sent while opening chat activity.
     */
    private final AbstractChat intent;

    private ArrayList<AbstractChat> activeChats;

    private FinishUpdateListener finishUpdateListener;

    private static final int TOTAL_COUNT = 200;
    private static final int OFFSET = TOTAL_COUNT / 2;

    private Fragment currentFragment;

    public ChatViewerAdapter(FragmentManager fragmentManager, BaseEntity chat, FinishUpdateListener finishUpdateListener) {
        super(fragmentManager);
        this.finishUpdateListener = finishUpdateListener;

        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());
        intent = MessageManager.getInstance().getOrCreateChat(chat.getAccount(), Jid.getBareAddress(chat.getUser()));

        updateChats();
    }

    public ChatViewerAdapter(FragmentManager fragmentManager, FinishUpdateListener finishUpdateListener) {
        super(fragmentManager);
        this.finishUpdateListener = finishUpdateListener;

        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());
        intent = null;
        updateChats();
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
            return RecentChatFragment.newInstance();
        }

        AbstractChat chat = activeChats.get(getChatIndexFromRealPosition(realPosition));
        return ChatViewerFragment.newInstance(chat.getAccount(), chat.getUser());
    }

    public boolean updateChats() {

        ArrayList<AbstractChat> newChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());

        if (intent != null && !newChats.contains(intent)) {
            newChats.add(intent);
        }

        Collections.sort(newChats, new Comparator<AbstractChat>() {
            @Override
            public int compare(AbstractChat lhs, AbstractChat rhs) {
                return lhs.getCreationTime().compareTo(rhs.getCreationTime());
            }
        });


        if (isChatsEquals(newChats)) {
            return false;
        }

        activeChats = newChats;
        notifyDataSetChanged();

        return true;
    }

    private boolean isChatsEquals(ArrayList<AbstractChat> newChats) {
        if (newChats.size() != activeChats.size()) {
            return false;
        }

        for (int i = 0; i < activeChats.size(); i++) {
            AbstractChat oldChat = activeChats.get(i);
            AbstractChat newChat = newChats.get(i);

            if (!oldChat.equals(newChat.getAccount(), newChat.getUser())) {
                return false;
            }
        }
        return true;
    }

    public int getPageNumber(BaseEntity chat) {
        int realPosition = 0;

        for (int chatIndex = 0; chatIndex < activeChats.size(); chatIndex++) {
            if (activeChats.get(chatIndex).equals(chat)) {
                realPosition = chatIndex + 1;
                break;
            }
        }

        return realPosition + OFFSET;
    }

    public AbstractChat getChatByPageNumber(int virtualPosition) {
        int realPosition = getRealPagePosition(virtualPosition);

        if (realPosition == 0) {
            return null;
        }
        return activeChats.get(getChatIndexFromRealPosition(realPosition));
    }


    public int getRealPagePosition(int virtualPosition) {
        int realCount = getRealCount();

        int pageNumber = abs(virtualPosition - OFFSET) % realCount;
        if (virtualPosition >= OFFSET) {
            return pageNumber;
        } else {
            return pageNumber == 0 ? 0 : realCount - pageNumber;
        }

    }

    private int getChatIndexFromRealPosition(int realPosition) {
        return realPosition - 1;
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);

        finishUpdateListener.onChatViewAdapterFinishUpdate();
    }

    public interface FinishUpdateListener {
        public void onChatViewAdapterFinishUpdate();
    }

    public ArrayList<AbstractChat> getActiveChats() {
        return activeChats;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        if (currentFragment instanceof ChatViewerFragment) {
            ((ChatViewerFragment)currentFragment).saveInputState();
        }

        currentFragment = (Fragment) object;
    }

    public Fragment getCurrentFragment() {
        return currentFragment;
    }

}