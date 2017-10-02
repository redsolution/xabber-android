package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.bookmarks.BookmarksManager;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.ServerInfoAdapter;
import com.xabber.android.ui.color.BarPainter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jivesoftware.smackx.csi.ClientStateIndicationManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muclight.MultiUserChatLightManager;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.push_notifications.PushNotificationsManager;
import org.jxmpp.jid.DomainBareJid;

import java.util.ArrayList;
import java.util.List;

public class ServerInfoActivity extends ManagedActivity {

    static final String LOG_TAG = ServerInfoActivity.class.getSimpleName();
    AccountItem accountItem;
    ServerInfoAdapter serverInfoAdapter;
    View progressBar;

    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, ServerInfoActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_info);

        final Intent intent = getIntent();

        AccountJid account = getAccount(intent);
        if (account == null) {
            finish();
            return;
        }

        accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem == null) {
            Application.getInstance().onError(R.string.NO_SUCH_ACCOUNT);
            finish();
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(accountItem.getConnection().getXMPPServiceDomain());

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.server_info_recycler_view);

        serverInfoAdapter = new ServerInfoAdapter();


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(serverInfoAdapter);


        progressBar = findViewById(R.id.server_info_progress_bar);

        requestServerInfo();
    }

    private void requestServerInfo() {
        progressBar.setVisibility(View.VISIBLE);

        Application.getInstance().runInBackgroundUserRequest(new Runnable() {
            @Override
            public void run() {
                final ServiceDiscoveryManager serviceDiscoveryManager
                        = ServiceDiscoveryManager.getInstanceFor(accountItem.getConnection());

                final List<String> serverInfo = getServerInfo(serviceDiscoveryManager);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        serverInfoAdapter.setServerInfoList(serverInfo);
                    }
                });
            }
        });
    }

    String getCheckOrCross(boolean flag) {
        if (flag) {
            return getString(R.string.check_mark);
        } else {
            return getString(R.string.cross_mark);
        }
    }

    @NonNull
    List<String> getServerInfo(ServiceDiscoveryManager serviceDiscoveryManager) {
        final List<String> serverInfoList = new ArrayList<>();

        XMPPTCPConnection connection = accountItem.getConnection();

        if (!connection.isAuthenticated()) {
            serverInfoList.add(getString(R.string.NOT_CONNECTED));
            return serverInfoList;
        }

        try {
            boolean muc = !MultiUserChatManager.getInstanceFor(connection).getXMPPServiceDomains().isEmpty();
            boolean pep = PEPManager.getInstanceFor(connection).isSupported();
            boolean blockingCommand = BlockingCommandManager.getInstanceFor(connection).isSupportedByServer();
            boolean sm = connection.isSmAvailable();
            boolean rosterVersioning = Roster.getInstanceFor(connection).isRosterVersioningSupported();
            boolean carbons = org.jivesoftware.smackx.carbons.CarbonManager.getInstanceFor(connection).isSupportedByServer();
            boolean mam = MamManager.getInstanceFor(connection).isSupportedByServer();
            boolean csi = ClientStateIndicationManager.isSupported(connection);
            boolean push = PushNotificationsManager.getInstanceFor(connection).isSupportedByServer();
            boolean fileUpload = HttpFileUploadManager.getInstance().isFileUploadSupported(accountItem.getAccount());
            boolean mucLight = !MultiUserChatLightManager.getInstanceFor(connection).getLocalServices().isEmpty();
            boolean bookmarks = BookmarksManager.getInstance().isSupported(accountItem.getAccount());

            serverInfoList.add(getString(R.string.xep_0045_muc) + " " + getCheckOrCross(muc));
            serverInfoList.add(getString(R.string.xep_0163_pep) + " " + getCheckOrCross(pep));
            serverInfoList.add(getString(R.string.xep_0191_blocking) + " " + getCheckOrCross(blockingCommand));
            serverInfoList.add(getString(R.string.xep_0198_sm) + " " + getCheckOrCross(sm));
            serverInfoList.add(getString(R.string.xep_0237_roster_ver) + " " + getCheckOrCross(rosterVersioning));
            serverInfoList.add(getString(R.string.xep_0280_carbons) + " " + getCheckOrCross(carbons));
            serverInfoList.add(getString(R.string.xep_0313_mam) + " " + getCheckOrCross(mam));
            serverInfoList.add(getString(R.string.xep_0352_csi) + " " + getCheckOrCross(csi));
            serverInfoList.add(getString(R.string.xep_0357_push) + " " + getCheckOrCross(push));
            serverInfoList.add(getString(R.string.xep_0363_file_upload) + " " + getCheckOrCross(fileUpload));
            serverInfoList.add(getString(R.string.xep_xxxx_muc_light) + " " + getCheckOrCross(mucLight));
            serverInfoList.add(getString(R.string.xep_0048_bookmarks) + " " + getCheckOrCross(bookmarks));
            serverInfoList.add("");
        } catch (InterruptedException | SmackException.NoResponseException
                | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
            LogManager.exception(LOG_TAG, e);
        }

        DomainBareJid xmppServiceDomain = connection.getXMPPServiceDomain();

        try {
            DiscoverInfo discoverInfo = serviceDiscoveryManager.discoverInfo(xmppServiceDomain);

            List<DiscoverInfo.Identity> identities = discoverInfo.getIdentities();

            if (!identities.isEmpty()) {
                serverInfoList.add(getString(R.string.identities));

                for (DiscoverInfo.Identity identity : identities) {
                    serverInfoList.add(identity.getCategory() + " " + identity.getType() + " " + identity.getName());
                }
                serverInfoList.add("");
            }

            if (!discoverInfo.getFeatures().isEmpty()) {
                serverInfoList.add(getString(R.string.features));

                for (DiscoverInfo.Feature feature : discoverInfo.getFeatures()) {
                    serverInfoList.add(feature.getVar());
                }
                serverInfoList.add("");
            }

            DiscoverItems items = serviceDiscoveryManager.discoverItems(xmppServiceDomain);

            if (!items.getItems().isEmpty()) {
                serverInfoList.add(getString(R.string.items));

                for (DiscoverItems.Item item : items.getItems()) {
                    serverInfoList.add(item.getEntityID().toString());
                }
            }

        } catch (InterruptedException | SmackException.NoResponseException
                | XMPPException.XMPPErrorException | SmackException.NotConnectedException e) {
            LogManager.exception(LOG_TAG, e);
        }

        if (serverInfoList.isEmpty()) {
            serverInfoList.add(getString(R.string.SERVER_INFO_ERROR));
        }

        return serverInfoList;
    }
}
