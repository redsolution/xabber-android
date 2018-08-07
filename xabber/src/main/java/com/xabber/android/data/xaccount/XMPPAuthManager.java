package com.xabber.android.data.xaccount;

import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnPacketListener;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;

import rx.subjects.PublishSubject;

public class XMPPAuthManager implements OnPacketListener {

    private static final String EXTENSION_NAMESPACE = "http://jabber.org/protocol/http-auth";
    private static final String ATTRIBUTE_ID = "id";

    private static XMPPAuthManager instance;
    private PublishSubject<String> authCodeSubscribe = PublishSubject.create();

    private String lastRequestId = null;
    private String apiJid = null;
    private String authCode;

    public static XMPPAuthManager getInstance() {
        if (instance == null) instance = new XMPPAuthManager();
        return instance;
    }

    public PublishSubject<String> subscribeForAuthCode() {
        return authCodeSubscribe;
    }

    public void updateRequestId(String requestId, String apiJid) {
        this.lastRequestId = requestId;
        this.apiJid = apiJid;
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {

        if (apiJid == null || lastRequestId == null) return;

        if (packet instanceof Message) {
            Message message = (Message) packet;
            if (apiJid.equals(message.getFrom().toString())) {
                if (lastRequestId.equals(message.getStanzaId())) {

                    StandardExtensionElement extensionElement = (StandardExtensionElement)
                            message.getExtension(EXTENSION_NAMESPACE);
                    this.authCode = extensionElement.getAttributeValue(ATTRIBUTE_ID);
                    authCodeSubscribe.onNext(this.authCode);
                }
            }
        }
    }
}
