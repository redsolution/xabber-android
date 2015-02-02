package com.xabber.android.ui.adapter;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.xabber.android.data.LogManager;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.ui.ChatViewerFragment;
import com.xabber.xmpp.address.Jid;

import java.util.ArrayList;
import java.util.Collection;

public class ChatViewerAdapter extends FragmentStatePagerAdapter implements UpdatableAdapter {

    /**
     * Intent sent while opening chat activity.
     */
    private final AbstractChat intent;

    /**
     * Position to insert intent.
     */
    private final int intentPosition;

    private ArrayList<AbstractChat> activeChats;

    private FinishUpdateListener finishUpdateListener;

    public ChatViewerAdapter(FragmentManager fragmentManager, String account, String user, FinishUpdateListener finishUpdateListener) {
        super(fragmentManager);
        this.finishUpdateListener = finishUpdateListener;

        activeChats = new ArrayList<>();
        intent = MessageManager.getInstance().getOrCreateChat(account,
                Jid.getBareAddress(user));
        Collection<? extends BaseEntity> activeChats = MessageManager
                .getInstance().getActiveChats();
        if (activeChats.contains(intent)) {
            intentPosition = -1;
        } else {
            intentPosition = activeChats.size();
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
        if (intentPosition != -1) {
            int index = activeChats.indexOf(intent);
            AbstractChat chat;
            if (index == -1) {
                chat = intent;
            } else {
                chat = activeChats.remove(index);
            }
            activeChats.add(Math.min(intentPosition, activeChats.size()), chat);
        }
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