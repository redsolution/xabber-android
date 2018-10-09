package com.xabber.android.data.extension.privatestorage;

import android.support.annotation.Nullable;

import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.adapter.XMPPAccountAuthAdapter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iqprivate.PrivateDataManager;
import org.jivesoftware.smackx.iqprivate.packet.PrivateData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Single;

public class PrivateStorageManager {

    private static final String NAMESPACE = "xabber:options";
    private static final String ELEMENT_NAME = "storage";
    private static final String TYPE_BIND = "bind";
    private static final String BIND_TRUE = "1";
    private static final String BIND_FALSE = "0";

    private static PrivateStorageManager instance;

    static {
        PrivateDataManager.addPrivateDataProvider(ELEMENT_NAME, NAMESPACE,
                new XabberOptionsPrivateData.Provider());
    }

    public static PrivateStorageManager getInstance() {
        if (instance == null) instance = new PrivateStorageManager();
        return instance;
    }

    public boolean haveXabberAccountBinding(AccountJid accountJid) {
        XabberOptionsPrivateData privateData = (XabberOptionsPrivateData) getPrivateData(accountJid, NAMESPACE, ELEMENT_NAME);
        if (privateData == null) return false;
        return BIND_TRUE.equals(privateData.getValue(TYPE_BIND));
    }

    public void setXabberAccountBinding(AccountJid accountJid, boolean bind) {
        XabberOptionsPrivateData privateData = new XabberOptionsPrivateData(ELEMENT_NAME, NAMESPACE);
        privateData.setValue(TYPE_BIND, bind ? BIND_TRUE : BIND_FALSE);
        setPrivateData(accountJid, privateData);
    }

    @Nullable private PrivateData getPrivateData(AccountJid accountJid, String namespace, String elementName) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem == null || !accountItem.isEnabled()) return null;

        XMPPTCPConnection connection = accountItem.getConnection();
        PrivateDataManager privateDataManager = PrivateDataManager.getInstanceFor(connection);
        try {
            if (!privateDataManager.isSupported()) return null;
            return privateDataManager.getPrivateData(elementName, namespace);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException
                | IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setPrivateData(AccountJid accountJid, PrivateData privateData) {
        AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
        if (accountItem == null || !accountItem.isEnabled()) return;

        XMPPTCPConnection connection = accountItem.getConnection();
        PrivateDataManager privateDataManager = PrivateDataManager.getInstanceFor(connection);

        try {
            if (!privateDataManager.isSupported()) return;
            privateDataManager.setPrivateData(privateData);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException
                | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /** Prepare views */

    public Single<List<XMPPAccountAuthAdapter.AccountView>> getAccountViewWithBindings(List<AccountJid> accounts) {
        return Single.fromCallable(new CallableLoadBindings(accounts));
    }

    private class CallableLoadBindings implements Callable<List<XMPPAccountAuthAdapter.AccountView>> {

        private List<AccountJid> accounts;

        public CallableLoadBindings(List<AccountJid> accounts) {
            this.accounts = accounts;
        }

        @Override
        public List<XMPPAccountAuthAdapter.AccountView> call() throws Exception {
            List<XMPPAccountAuthAdapter.AccountView> items = new ArrayList<>();
            for (AccountJid accountJid : accounts) {
                items.add(new XMPPAccountAuthAdapter.AccountView(accountJid,
                        haveXabberAccountBinding(accountJid)));
            }
            return items;
        }
    }

}
