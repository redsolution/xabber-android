package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.adapter.ServerInfoAdapter;
import com.xabber.android.ui.color.BarPainter;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
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
                NavUtils.navigateUpFromSameTask(ServerInfoActivity.this);
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

                List<String> serverInfoList = null;

                try {
                    serverInfoList = getServerInfo(serviceDiscoveryManager);
                } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                        | SmackException.NotConnectedException | InterruptedException e) {
                    LogManager.exception(LOG_TAG, e);
                }

                final List<String> finalServerInfoList = serverInfoList;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        serverInfoAdapter.setServerInfoList(finalServerInfoList);
                    }
                });
            }
        });
    }

    @NonNull
    List<String> getServerInfo(ServiceDiscoveryManager serviceDiscoveryManager)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException {
        final List<String> serverInfoList = new ArrayList<>();

        DomainBareJid xmppServiceDomain = accountItem.getConnection().getXMPPServiceDomain();

        DiscoverInfo discoverInfo = serviceDiscoveryManager.discoverInfo(xmppServiceDomain);

        List<DiscoverInfo.Identity> identities = discoverInfo.getIdentities();

        serverInfoList.add("Identities:");

        for (DiscoverInfo.Identity identity : identities) {
            serverInfoList.add(identity.getCategory() + " " + identity.getType() + " " + identity.getName());
        }


        serverInfoList.add("");
        serverInfoList.add("Features:");

        for (DiscoverInfo.Feature feature : discoverInfo.getFeatures()) {
            serverInfoList.add(feature.getVar());
        }

        DiscoverItems items = serviceDiscoveryManager.discoverItems(xmppServiceDomain);

        items.getItems();

        serverInfoList.add("");
        serverInfoList.add("Items:");

        for (DiscoverItems.Item item : items.getItems()) {
            serverInfoList.add(item.getEntityID().toString());
       }
        return serverInfoList;
    }
}
