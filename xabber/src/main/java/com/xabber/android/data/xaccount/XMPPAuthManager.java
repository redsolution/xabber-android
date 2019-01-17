package com.xabber.android.data.xaccount;

import android.util.Log;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.privatestorage.PrivateStorageManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class XMPPAuthManager implements OnPacketListener, OnConnectedListener {

    private static final String LOG_TAG = XMPPAuthManager.class.getSimpleName();
    private static final String URL_AUTH = "https://www.xabber.com/account/auth/login/";

    private static XMPPAuthManager instance;

    private Map<String, Request> requests = new HashMap<>();

    public static XMPPAuthManager getInstance() {
        if (instance == null) instance = new XMPPAuthManager();
        return instance;
    }

    public void addRequest(String requestId, String apiJid, String clientJid) {
        onRequestReceived(new Request(requestId, clientJid, apiJid));
        addContactToRoster(apiJid, clientJid);
    }

    /**
     *  StanzaId is RequestId
     * */
    public boolean isXabberServiceMessage(String stanzaId) {
        return requests.containsKey(stanzaId);
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof HttpConfirmIq && URL_AUTH.equals(((HttpConfirmIq) packet).getUrl())) {
            HttpConfirmIq httpConfirmIq = (HttpConfirmIq) packet;

            String apiJid = packet.getFrom().toString();
            String clientJid = packet.getTo().toString();
            String requestId = packet.getStanzaId();
            String code = httpConfirmIq.getId();

            if (requestId != null && code != null)
                onRequestReceived(new Request(requestId, clientJid, apiJid, code));
        }
    }

    @Override
    public void onConnected(final ConnectionItem connection) {


        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                XabberAccount xabberAccount = XabberAccountManager.getInstance().getAccount();
                AccountJid accountJid = connection.getAccount();
                if (xabberAccount == null) {
                    AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                    if (accountItem != null && accountItem.isXabberAutoLoginEnabled()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (PrivateStorageManager.getInstance().haveXabberAccountBinding(accountJid))
                            requestXMPPAuthCode(accountJid);
                    }

                } else if (xabberAccount.getFullUsername()
                        .equals(AccountManager.getInstance().getVerboseName(accountJid))) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    PrivateStorageManager.getInstance().setXabberAccountBinding(accountJid, true);
                }
            }
        });
    }

    private void requestXMPPAuthCode(final AccountJid accountJid) {
        Log.d(LOG_TAG, "request XMPP code for account: "
                + accountJid.getFullJid().toString());
        AuthManager.requestXMPPCode(accountJid.getFullJid().toString())
            .subscribe(new Action1<AuthManager.XMPPCode>() {
                @Override
                public void call(AuthManager.XMPPCode code) {
                    Log.d(XMPPAuthManager.class.toString(), "xmpp auth code requested successfully");
                    addRequest(code.getRequestId(), code.getApiJid(), accountJid.getFullJid().toString());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d(XMPPAuthManager.class.toString(), "request XMPP code failed: " + throwable.toString());
                }
            });
    }

    private void confirmXMPP(Request request) {
        Log.d(LOG_TAG, "confirm account: " + request.clientJid);
        AuthManager.confirmXMPP(request.clientJid, request.code)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<XabberAccount>() {
                @Override
                public void call(XabberAccount account) {
                    Log.d(XMPPAuthManager.class.toString(), "xabber account authorized successfully");
                    updateSettings();
                    AccountManager.getInstance().setAllAccountAutoLoginToXabber(true);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d(XMPPAuthManager.class.toString(), "XMPP authorization failed: " + throwable.toString());
                }
            });
    }

    protected void updateSettings() {
        AuthManager.patchClientSettings(XabberAccountManager.getInstance().createSettingsList())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<XMPPAccountSettings>>() {
                @Override
                public void call(List<XMPPAccountSettings> s) {
                    Log.d(XMPPAuthManager.class.toString(),
                            "xabber account settings updated successfully");
                    XabberAccountManager.getInstance().registerEndpoint();
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d(XMPPAuthManager.class.toString(),
                            "xabber account settings update failed: " + throwable.toString());
                }
            });
    }

    private void onRequestReceived(Request request) {
        if (requests.containsKey(request.id)) {
            if (request.code == null) request.code = requests.get(request.id).code;
            confirmXMPP(request);
        } else requests.put(request.id, request);
    }

    private void addContactToRoster(String apiJid, String clientJid) {
        UserJid user;
        AccountJid account;
        try {
            user = UserJid.from(apiJid);
            account = AccountJid.from(clientJid);

            RosterManager.getInstance().createContact(account, user,
                    "xabber", Collections.EMPTY_LIST);
            PresenceManager.getInstance().requestSubscription(account, user);

        } catch (UserJid.UserJidCreateException | XmppStringprepException | InterruptedException |
                SmackException | NetworkException | XMPPException.XMPPErrorException e) {
            LogManager.exception(this, e);
            return;
        }
    }

    class Request {
        private String id;
        private String clientJid;
        private String apiJid;
        private String code;

        public Request(String id, String clientJid, String apiJid) {
            this.id = id;
            this.clientJid = clientJid;
            this.apiJid = apiJid;
        }

        public Request(String id, String clientJid, String apiJid, String code) {
            this.id = id;
            this.clientJid = clientJid;
            this.apiJid = apiJid;
            this.code = code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getId() {
            return id;
        }

        public String getClientJid() {
            return clientJid;
        }

        public String getApiJid() {
            return apiJid;
        }

        public String getCode() {
            return code;
        }
    }
}
