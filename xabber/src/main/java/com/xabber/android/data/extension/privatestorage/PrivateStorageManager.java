package com.xabber.android.data.extension.privatestorage;

import androidx.annotation.Nullable;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.XMPPAccountAuthAdapter;
import com.xabber.xmpp.smack.XMPPTCPConnection;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.iqprivate.PrivateDataManager;
import org.jivesoftware.smackx.iqprivate.packet.PrivateData;
import org.jivesoftware.smackx.iqprivate.packet.PrivateDataIQ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Single;

public class PrivateStorageManager {

    private static final String LOG_TAG = PrivateStorageManager.class.getSimpleName();

    private static final String NAMESPACE = "xabber:options";
    private static final String ELEMENT_NAME = "storage";
    private static final String TYPE_BIND = "bind";
    private static final String BIND_TRUE = "1";
    private static final String BIND_FALSE = "0";

    private static PrivateStorageManager instance;

    static {
        PrivateDataManager.addPrivateDataProvider(ELEMENT_NAME, NAMESPACE, new XabberOptionsPrivateData.Provider());
    }

    public static PrivateStorageManager getInstance() {
        if (instance == null) instance = new PrivateStorageManager();
        return instance;
    }

    public boolean haveXabberAccountBinding(AccountJid accountJid) {
        XabberOptionsPrivateData privateData =
                (XabberOptionsPrivateData) getPrivateData(accountJid, NAMESPACE, ELEMENT_NAME);
        if (privateData == null) return false;
        return BIND_TRUE.equals(privateData.getValue(TYPE_BIND));
    }

    public void setXabberAccountBinding(AccountJid accountJid, boolean bind) {
        XabberOptionsPrivateData privateData = new XabberOptionsPrivateData(ELEMENT_NAME, NAMESPACE);
        privateData.setValue(TYPE_BIND, bind ? BIND_TRUE : BIND_FALSE);
        Application.getInstance().runInBackgroundNetworkUserRequest(() -> setPrivateData(accountJid, privateData));
    }

    @Nullable
    private PrivateData getPrivateData(AccountJid accountJid, String namespace, String elementName) {
        AccountItem accountItem = AccountManager.INSTANCE.getAccount(accountJid);
        if (accountItem == null || !accountItem.isEnabled()) return null;

        XMPPTCPConnection connection = accountItem.getConnection();
        PrivateDataManager privateDataManager = PrivateDataManager.getInstanceFor(connection);
        try {
            if (!privateDataManager.isSupported()) return null;
            return privateDataManager.getPrivateData(elementName, namespace);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException | IllegalArgumentException e) {
            LogManager.exception(LOG_TAG, e);
            return null;
        }
    }

    private void setPrivateData(AccountJid accountJid, PrivateData privateData) {
        AccountItem accountItem = AccountManager.INSTANCE.getAccount(accountJid);
        if (accountItem == null || !accountItem.isEnabled()) return;

        XMPPTCPConnection connection = accountItem.getConnection();

        try {
            IQ privateDataSet = new PrivateDataIQ(privateData);
            connection.sendStanza(privateDataSet);
        } catch (SmackException.NotConnectedException | InterruptedException | IllegalArgumentException e) {
            LogManager.exception(LOG_TAG, e);
        }
    }

    /** Prepare views */
    public Single<List<XMPPAccountAuthAdapter.AccountView>> getAccountViewWithBindings(List<AccountJid> accounts) {
        return Single.fromCallable(new CallableLoadBindings(accounts));
    }

    private class CallableLoadBindings implements Callable<List<XMPPAccountAuthAdapter.AccountView>> {

        private final List<AccountJid> accounts;

        public CallableLoadBindings(List<AccountJid> accounts) {
            this.accounts = accounts;
        }

        @Override
        public List<XMPPAccountAuthAdapter.AccountView> call() {
            List<XMPPAccountAuthAdapter.AccountView> items = new ArrayList<>();
            for (AccountJid accountJid : accounts) {
                items.add(new XMPPAccountAuthAdapter.AccountView(accountJid, haveXabberAccountBinding(accountJid)));
            }
            return items;
        }
    }

}
