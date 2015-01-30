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

public class ChatViewerAdapter extends FragmentStatePagerAdapter implements UpdatableAdapter {

    private ArrayList<AbstractChat> activeChats;

    public ChatViewerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        LogManager.i(this, "ChatViewerAdapter");

        onChange();
    }

    @Override
    public int getCount() {
        return activeChats.size();
    }

    @Override
    public Fragment getItem(int i) {
        LogManager.i(this, "getItem: " + i);


        AbstractChat abstractChat = getChat(i);

        return ChatViewerFragment.newInstance(abstractChat.getAccount(), abstractChat.getUser());
    }

    public AbstractChat getChat(int i) {
        return activeChats.get(i);
    }

    @Override
    public void onChange() {
        LogManager.i(this, "onChange: ");

        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());
        notifyDataSetChanged();
    }

    public int getPosition(String account, String user) {
        LogManager.i(this, "getPosition: " + account + " : " + user);

        activeChats = new ArrayList<>(MessageManager.getInstance().getActiveChats());

        for (int position = 0; position < activeChats.size(); position++) {
            if (activeChats.get(position).equals(account, user)) {
                return position;
            }
        }

        LogManager.i(this, "creating new chat: " + account + " : " + user);
        AbstractChat chat = MessageManager.getInstance().getOrCreateChat(account, Jid.getBareAddress(user));
        activeChats.add(chat);
        notifyDataSetChanged();

        return activeChats.indexOf(chat);
    }


    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        LogManager.i(this, "setPrimaryItem position: " + position);
    }

}