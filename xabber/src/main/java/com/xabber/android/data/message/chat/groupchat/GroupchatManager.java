package com.xabber.android.data.message.chat.groupchat;

import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.database.repositories.ChatRepository;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.util.Collection;

//todo add and implement other listeners

public class GroupchatManager implements OnLoadListener {

    private static final String LOG_TAG = GroupchatManager.class.getSimpleName();
    private static final String NAMESPACE = "http://xabber.com/protocol/groupchat";

    static private GroupchatManager instance;

    private Collection<GroupChat> groupChatsList;

    public static GroupchatManager getInstance(){
        if (instance == null)
            instance = new GroupchatManager();
        return instance;
    }

    @Override
    public void onLoad() {
        for (AbstractChat abstractChat : ChatRepository.getAllChatsFromRealm()){
            if (abstractChat.isGroupchat()){
                groupChatsList.add((GroupChat) abstractChat);
            }
        }
    }

    public boolean isSupported(XMPPTCPConnection connection) {
        try {
            return ServiceDiscoveryManager.getInstanceFor(connection).serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(AccountJid accountJid) {
        return isSupported(AccountManager.getInstance().getAccount(accountJid).getConnection());
    }

}
