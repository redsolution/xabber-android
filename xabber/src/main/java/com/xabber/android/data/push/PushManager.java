package com.xabber.android.data.push;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.xabber.android.BuildConfig;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionItem;
import com.xabber.android.data.connection.listeners.OnConnectedListener;
import com.xabber.android.data.connection.listeners.OnPacketListener;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.PushLogRecord;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.http.PushApiClient;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.utils.ExternalAPIs;
import com.xabber.android.utils.Utils;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import com.xabber.xmpp.smack.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.push_notifications.element.EnablePushNotificationsIQ;
import org.jivesoftware.smackx.push_notifications.element.PushNotificationsElements;
import org.jxmpp.jid.EntityBareJid;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import okhttp3.ResponseBody;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PushManager implements OnConnectedListener, OnPacketListener {

    private static final String LOG_TAG = PushManager.class.getSimpleName();

    private static PushManager instance;

    public static PushManager getInstance() {
        if (instance == null)
            instance = new PushManager();
        return instance;
    }

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private HashMap<String, Boolean> waitingIQs = new HashMap<>();

    /** Listeners */

    @Override
    public void onConnected(final ConnectionItem connection) {
        Application.getInstance().runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AccountJid accountJid = connection.getAccount();
                AccountItem accountItem = AccountManager.getInstance().getAccount(accountJid);
                enablePushNotificationsIfNeed(accountItem);
            }
        });
    }

    public void onEndpointRegistered(String jid, String pushServiceJid, String node) {
        LogManager.d(LOG_TAG, "Received endpoint-registered push. Send push enable iq.");
        AccountJid accountJid = null;
        Collection<AccountJid> accounts = AccountManager.getInstance().getEnabledAccounts();
        for (AccountJid account : accounts) {
            if ((account.getFullJid().asBareJid() + getAndroidId()).equals(jid)) {
                accountJid = account;
                break;
            }
        }

        if (accountJid != null) {
            AccountItem account = AccountManager.getInstance().getAccount(accountJid);
            if (account != null) {

                // save node to account
                AccountManager.getInstance().setPushNode(account, node, pushServiceJid);

                // update push nodes
                updateEnabledPushNodes();

                // enable push on XMPP-server
                sendEnablePushIQ(account, pushServiceJid, node);
            }
        }
    }

    public void onNewMessagePush(Context context, String node) {
        String message;
        if (!Application.getInstance().isServiceStarted()
                && SettingsManager.getEnabledPushNodes().contains(node)) {
            Utils.startXabberServiceCompatWithSyncMode(context, node);
            message = "Starting service";
        } else if (SyncManager.getInstance().isSyncMode()) {
            message = "Service also started. Add account to allowed accounts";
            SyncManager.getInstance().addAllowedAccount(node);
        } else message = "Service also started. Not a sync mode - account maybe connected";

        LogManager.d(LOG_TAG, "Received message push. " + message);
        if (BuildConfig.FLAVOR.equals("dev")) addToPushLog(message);
    }

    @Override
    public void onStanza(ConnectionItem connection, Stanza packet) {
        if (packet instanceof IQ && ((IQ) packet).getType() != IQ.Type.error) {
            if (waitingIQs.containsKey(packet.getStanzaId())) {

                AccountJid account = connection.getAccount();
                AccountItem accountItem = AccountManager.getInstance().getAccount(account);
                Boolean enable = waitingIQs.get(packet.getStanzaId());
                if (accountItem != null && enable != null) {
                    AccountManager.getInstance().setPushWasEnabled(accountItem, enable);
                }
                waitingIQs.remove(packet.getStanzaId());
            }
        }
    }

    /** Api */

    public void enablePushNotificationsIfNeed(AccountItem accountItem) {
        if (accountItem != null && accountItem.isPushEnabled() && accountItem.getConnection().isConnected()) {
            if (isSupport(accountItem.getConnection())) {
                registerEndpoint(accountItem.getAccount());
            } else AccountManager.getInstance().setPushWasEnabled(accountItem, false);
        }
    }

    public void disablePushNotification(AccountItem accountItem, boolean needConfirm) {
        if (accountItem != null) {
            //deleteEndpoint(accountItem);  //Disable to avoid sending annoying requests to PUSH-server
            AccountManager.getInstance().setPushWasEnabled(accountItem, false);
        }
    }

    public void updateEnabledPushNodes() {
        StringBuilder stringBuilder = new StringBuilder();
        for (AccountItem accountItem : AccountManager.getInstance().getAllAccountItems()) {
            String node = accountItem.getPushNode();
            if (accountItem.isEnabled() && accountItem.isPushEnabled()
                    && accountItem.isPushWasEnabled() && node != null && !node.isEmpty()) {
                stringBuilder.append(node);
                stringBuilder.append(" ");
            }
        }
        SettingsManager.setEnabledPushNodes(stringBuilder.toString());
    }

    public boolean isSupport(XMPPTCPConnection connection) {
        try {
            EntityBareJid jid = connection.getUser().asEntityBareJid();
            return ServiceDiscoveryManager.getInstanceFor(connection)
                    .supportsFeatures(jid, Collections.singletonList(PushNotificationsElements.NAMESPACE));

        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                | SmackException.NotConnectedException | InterruptedException e) {
            return false;
        }
    }

    /** Log */

    private void addToPushLog(String message) {
        Realm realm = RealmManager.getInstance().getNewBackgroundRealm();
        PushLogRecord pushLogRecord = new PushLogRecord(System.currentTimeMillis(), message);
        realm.beginTransaction();
        realm.copyToRealm(pushLogRecord);
        realm.commitTransaction();
        realm.close();
    }

    public static List<String> getPushLogs() {
        Realm realm = RealmManager.getInstance().getRealmUiThread();
        List<String> logs = new ArrayList<>();
        RealmResults<PushLogRecord> records = realm.where(PushLogRecord.class)
                .findAllSorted(PushLogRecord.Fields.TIME, Sort.DESCENDING);
        for (PushLogRecord record : records) {
            String time = new SimpleDateFormat("yyyy.MM.dd - HH:mm:ss",
                    Locale.getDefault()).format(new Date(record.getTime()));
            logs.add(time + ": " + record.getMessage());
        }
        return logs;
    }

    public static void clearPushLog() {
        Realm realm = RealmManager.getInstance().getRealmUiThread();
        RealmResults<PushLogRecord> records = realm.where(PushLogRecord.class).findAll();
        realm.beginTransaction();
        records.deleteAllFromRealm();
        realm.commitTransaction();
    }

    /** Private */

    private void registerEndpoint(AccountJid accountJid) {
        String token = ExternalAPIs.getPushEndpointToken();
        if (token == null) return;

        compositeSubscription.add(
                PushApiClient.registerEndpoint(
                        token,
                        accountJid.getFullJid().asBareJid().toString() + getAndroidId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<ResponseBody>() {
                            @Override
                            public void call(ResponseBody responseBody) {
                                Log.d(LOG_TAG, "Endpoint successfully registered");
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.d(LOG_TAG, "Endpoint register failed: " + throwable.toString());
                            }
                        }));
    }

    private void deleteEndpoint(final AccountItem accountItem) {
        String token = ExternalAPIs.getPushEndpointToken();
        if (token == null) return;

        compositeSubscription.add(
                PushApiClient.deleteEndpoint(
                        token, accountItem.getAccount().toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<ResponseBody>() {
                            @Override
                            public void call(ResponseBody responseBody) {
                                Log.d(LOG_TAG, "Endpoint successfully unregistered");
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.d(LOG_TAG, "Endpoint unregister failed: " + throwable.toString());
                            }
                        }));
    }

    private void sendEnablePushIQ(final AccountItem accountItem, final String pushServiceJid, final String node) {
        String stanzaID = null;
        try {
            EnablePushNotificationsIQ enableIQ = new EnablePushNotificationsIQ(
                    UserJid.from(pushServiceJid).getJid(), node, null);
            stanzaID = enableIQ.getStanzaId();
            waitingIQs.put(stanzaID, true);
            accountItem.getConnection().sendStanza(enableIQ);
        } catch (SmackException.NotConnectedException | InterruptedException | UserJid.UserJidCreateException e) {
            Log.d(LOG_TAG, "Push notification enabling failed: " + e.toString());
            waitingIQs.remove(stanzaID);
            AccountManager.getInstance().setPushWasEnabled(accountItem, false);
        }
    }

    @SuppressLint("HardwareIds")
    private static String getAndroidId() {
        return "/" + Settings.Secure.getString(Application.getInstance().getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

}
