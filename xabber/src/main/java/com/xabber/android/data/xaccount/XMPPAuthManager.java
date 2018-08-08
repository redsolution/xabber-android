package com.xabber.android.data.xaccount;

import android.util.Log;

import com.xabber.android.data.Application;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.privatestorage.PrivateStorageManager;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class XMPPAuthManager implements OnPacketListener, OnConnectedListener {

    private static final String EXTENSION_NAMESPACE = "http://jabber.org/protocol/http-auth";
    private static final String ATTRIBUTE_ID = "id";

    private static XMPPAuthManager instance;
    private PublishSubject<String> authCodeSubscribe = PublishSubject.create();
    private List<AccountJid> accountsForCheck = new ArrayList<>();

    private String lastRequestId = null;
    private String apiJid = null;
    private String authCode;
    private AccountJid authJid;

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

    public void addAccountForCheck(AccountJid accountJid) {
        accountsForCheck.add(accountJid);
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

                    // if login running in background
                    if (authJid != null && authCode != null) confirmXMPP(authJid, authCode);
                }
            }
        }
    }

    @Override
    public void onConnected(final ConnectionItem connection) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                if (XabberAccountManager.getInstance().getAccount() == null) {
                    AccountJid accountJid = connection.getAccount();
                    if (accountsForCheck.contains(accountJid)) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (PrivateStorageManager.getInstance().haveXabberAccountBinding(accountJid))
                            requestXMPPAuthCode(accountJid);
                        // TODO: 07.08.18 включить удаление аккаунта из очереди после попытки авториазации?
                        //accountsForCheck.remove(accountJid);
                    }
                }
            }
        });
    }

    private void requestXMPPAuthCode(final AccountJid accountJid) {
        Log.d(XMPPAuthManager.class.toString(), "request XMPP code for account: "
                + accountJid.getFullJid().toString());
        AuthManager.requestXMPPCode(accountJid.getFullJid().toString())
            .subscribe(new Action1<AuthManager.XMPPCode>() {
                @Override
                public void call(AuthManager.XMPPCode code) {
                    Log.d(XMPPAuthManager.class.toString(), "xmpp auth code requested successfully");
                    updateRequestId(code.getRequestId(), code.getApiJid());
                    authJid = accountJid;
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d(XMPPAuthManager.class.toString(), "request XMPP code failed: " + throwable.toString());
                }
            });
    }

    private void confirmXMPP(AccountJid accountJid, String code) {
        Log.d(XMPPAuthManager.class.toString(), "confirm account: "
                + accountJid.getFullJid().toString());
        AuthManager.confirmXMPP(accountJid.getFullJid().toString(), code)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<XabberAccount>() {
                @Override
                public void call(XabberAccount account) {
                    Log.d(XMPPAuthManager.class.toString(), "xabber account authorized successfully");
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d(XMPPAuthManager.class.toString(), "XMPP authorization failed: " + throwable.toString());
                }
            });
    }
}
