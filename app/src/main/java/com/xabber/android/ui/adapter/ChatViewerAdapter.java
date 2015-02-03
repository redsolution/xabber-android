package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.xabber.android.data.LogManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.xmpp.address.Jid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ChatViewerAdapter extends FragmentStatePagerAdapter implements UpdatableAdapter {

    /**
     * Intent sent while opening chat activity.
     */
    private final AbstractChat intent;

    /**
     * Position to insert intent.
     */
//    private final int intentPosition;

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
        return activeChats.size();
    }

    @Override
    public Fragment getItem(int i) {

        AbstractChat abstractChat = getChat(i);

        return ChatViewerFragment.newInstance(abstractChat.getAccount(), abstractChat.getUser());
    }

    public AbstractChat getChat(int i) {
        return activeChats.get(i);
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
        LogManager.i(this, "getPosition: " + account + " : " + user);

        for (int position = 0; position < activeChats.size(); position++) {
            if (activeChats.get(position).equals(account, user)) {
                return position;
            }
        }

        return -1;
    }


    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);
        finishUpdateListener.onChatViewAdapterFinishUpdate();
    }
    public interface FinishUpdateListener {
        public void onChatViewAdapterFinishUpdate();
    }
}