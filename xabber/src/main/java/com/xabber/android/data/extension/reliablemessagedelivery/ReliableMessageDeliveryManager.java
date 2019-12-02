package com.xabber.android.data.extension.reliablemessagedelivery;

import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.Jid;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ReliableMessageDeliveryManager implements OnPacketListener {

    public static final String NAMESPACE = "http://xabber.com/protocol/delivery";
    public static final String LOG_TAG = ReliableMessageDeliveryManager.class.getSimpleName();

    private LinkedHashMap<UserJid, LinkedList<String>> usersMessagesReceiptsWaiting = new LinkedHashMap<>();

    private static ReliableMessageDeliveryManager instance;

    public static ReliableMessageDeliveryManager getInstance(){
        if (instance == null)
            instance = new ReliableMessageDeliveryManager();
        return instance;
    }

    public boolean isSupported(XMPPTCPConnection xmpptcpConnection){
        try {
            return ServiceDiscoveryManager.getInstanceFor(xmpptcpConnection).serverSupportsFeature(NAMESPACE);
        } catch (Exception e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    public boolean isSupported(Jid jid){
        //TODO design method
        return false;
    }

    public void addMessageIdToReceiptWaiting(UserJid userJid, String messageId){
        if (usersMessagesReceiptsWaiting.containsKey(userJid))
            usersMessagesReceiptsWaiting.get(userJid).add(messageId);
        else {
            usersMessagesReceiptsWaiting.put(userJid, new LinkedList<String>());
            usersMessagesReceiptsWaiting.get(userJid).add(messageId);
        }
    }

    private UserJid getUserJidBymessageId(String id) throws NoSuchFieldException{
        for (LinkedList<String> list : usersMessagesReceiptsWaiting.values()){
            if (list.contains(id))
                for (Map.Entry<UserJid, LinkedList<String>> entry: usersMessagesReceiptsWaiting.entrySet())
                    if (entry.getValue().equals(list))
                        return entry.getKey();
        }
        throw new NoSuchFieldException("Can't find message in waiting for receipt list with provided id");
    }

    private void deleteMessageFromWaitingReceiptsList(String id) throws NoSuchFieldException{
        for (LinkedList<String> list : usersMessagesReceiptsWaiting.values()){
            if (list.contains(id)){
                list.remove(id);
                return;
            }
        }
        throw new NoSuchFieldException("Can't find message in waiting for recipt list with provided id");
    }

    private void onReceiptRecieved(Stanza stanza){
        //TODO this
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (stanza instanceof Message && ((Message) stanza).getType().equals(Message.Type.headline)){
            //TODO this
        }
    }

}
