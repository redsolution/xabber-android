package com.xabber.android.data.extension.reliablemessagedelivery;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.MessageUpdateEvent;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ReliableMessageDeliveryManager implements OnPacketListener {

    public static final String NAMESPACE = "http://xabber.com/protocol/delivery";
    public static final String LOG_TAG = ReliableMessageDeliveryManager.class.getSimpleName();

    private LinkedHashMap<AccountJid, LinkedList<String>> usersMessagesReceiptsWaiting = new LinkedHashMap<>();

    private static ReliableMessageDeliveryManager instance;

    public static ReliableMessageDeliveryManager getInstance(){
        if (instance == null)
            instance = new ReliableMessageDeliveryManager();
        ProviderManager.addExtensionProvider(ReceiptElement.ELEMENT, ReceiptElement.NAMESPACE, new ReceiptElement.ReceiptElementProvider());
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

    public boolean isSupported(AccountItem accountItem){
        return isSupported(accountItem.getConnection());
    }

    public void addMessageIdToReceiptWaiting(AccountJid accountJid, String messageId){
        if (usersMessagesReceiptsWaiting.containsKey(accountJid))
            usersMessagesReceiptsWaiting.get(accountJid).add(messageId);
        else {
            usersMessagesReceiptsWaiting.put(accountJid, new LinkedList<String>());
            usersMessagesReceiptsWaiting.get(accountJid).add(messageId);
        }

        LogManager.d(LOG_TAG, "Added to waiting list: " + accountJid.toString() + " " + messageId);
    }

    private AccountJid getAccountJidBymessageId(String id) throws NoSuchFieldException{
        for (LinkedList<String> list : usersMessagesReceiptsWaiting.values()){
            if (list.contains(id))
                for (Map.Entry<AccountJid, LinkedList<String>> entry: usersMessagesReceiptsWaiting.entrySet())
                    if (entry.getValue().equals(list))
                        return entry.getKey();
        }
        throw new NoSuchFieldException("Can't find message in waiting for receipt list with provided id: " + id);
    }

    private void deleteMessageFromWaitingReceiptsList(String id) throws NoSuchFieldException{
        for (LinkedList<String> list : usersMessagesReceiptsWaiting.values()){
            if (list.contains(id)){
                list.remove(id);
                return;
            }
        }
        throw new NoSuchFieldException("Can't find message in waiting for receipt list with provided id: " + id);
    }

    private void onReceiptRecieved(String time, String originId, String stanzaId){
        try {
            AccountJid accountJid = getAccountJidBymessageId(originId);
            deleteMessageFromWaitingReceiptsList(originId);
            //TODO writing to DB new stanza id and timestamp;
            LogManager.d(LOG_TAG, "Got receipt for account: " + accountJid.toString());
            EventBus.getDefault().post(new MessageUpdateEvent()); //TODO user and account into constructor;
        } catch (Exception e){
            LogManager.exception(LOG_TAG, e);
        }
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza stanza) {
        if (stanza instanceof Message
                && ((Message) stanza).getType().equals(Message.Type.headline )
                && stanza.hasExtension(NAMESPACE)){
            ReceiptElement receipt = (ReceiptElement) stanza.getExtension(NAMESPACE);
            onReceiptRecieved(receipt.getTimeElement().getStamp(),
                    receipt.getOriginIdElement().getId(),
                    receipt.getStanzaIdElement().getId());
        }
    }

}
