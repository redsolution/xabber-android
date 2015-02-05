package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.xmpp.address.Jid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChatViewerAdapter extends FragmentStatePagerAdapter implements UpdatableAdapter,
        ChatViewerFragment.ActiveChatProvider {

    /**
     * Intent sent while opening chat activity.
     */
    private final AbstractChat intent;

    private ArrayList<AbstractChat> activeChats;

    private FinishUpdateListener finishUpdateListener;

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
        return Integer.MAX_VALUE;
    }

    public int getRealCount() {
        int realCount = activeChats.size();
        if (realCount == 1) {
            return 4;
        } else if (realCount == 2 || realCount == 3) {
            return realCount * 2;
        } else {
            return realCount;
        }
    }

    @Override
    public Fragment getItem(int i) {
        int position = i % activeChats.size();

        ChatViewerFragment chatViewerFragment = ChatViewerFragment.newInstance();
        chatViewerFragment.setActiveChat(this, position);
        return chatViewerFragment;
    }

    public AbstractChat getChat(int i) {
        int realCount = activeChats.size();

        int position;

        if (realCount == 1) {
            position = 0;
        } else if (realCount == 2 || realCount == 3) {
            position = i % realCount;
        } else {
            position = i;
        }

        return activeChats.get(position);
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

    public int getPosition(String account, String user) {
        for (int position = 0; position < activeChats.size(); position++) {
            if (activeChats.get(position).equals(account, user)) {
                return position;
            }
        }

        return -1;
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);
        finishUpdateListener.onChatViewAdapterFinishUpdate();
    }

    @Override
    public AbstractChat getActiveChat(int index) {
        return getChat(index);
    }

    public interface FinishUpdateListener {
        public void onChatViewAdapterFinishUpdate();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}