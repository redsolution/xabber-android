package com.xabber.android.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.ui.OnAccountChangedListener;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.color.ColorManager;
import com.xabber.android.utils.Utils;

import org.jetbrains.annotations.Nullable;
import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConnectedDevicesActivity extends ManagedActivity implements OnAccountChangedListener {

    private Toolbar toolbar;
    private LinearLayout xmppItems;
    private BarPainter barPainter;

    private AccountJid account;

    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, ConnectedDevicesActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);

        final Intent intent = getIntent();
        account = getAccount(intent);
        if (account == null) {
            finish();
            return;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.account_connected_devices);

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);
        xmppItems = (LinearLayout) findViewById(R.id.xmpp_items);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        showOnlineAccounts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    private void showOnlineAccounts() {
        xmppItems.removeAllViews();
        List<View> resourcesList = new ArrayList<>();
        fillResourceList(resourcesList);
        //if (!resourcesList.isEmpty()) {
        //    addHeader(xmppItems, getString(R.string.contact_info_connected_clients_header));
        //}
        addItemGroup(resourcesList, xmppItems, R.drawable.ic_vcard_jabber_24dp, false);
    }

    private void addHeader(LinearLayout rootView, String text) {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View contactInfoHeader = inflater.inflate(R.layout.item_contact_info_header, rootView, false);
        TextView headerView = (TextView) contactInfoHeader.findViewById(R.id.contact_info_header_text_view);
        headerView.setTextColor(getResources().getColor(R.color.android_default_accent_color));
        headerView.setText(text);

        rootView.addView(contactInfoHeader);
    }

    private void fillResourceList(List<View> resourcesList) {
        List<Presence> allAccountPresences = PresenceManager.getInstance().getAvailableAccountPresences(account);

        Resourcepart accountResource = null;
        AccountItem accountItem = AccountManager.getInstance().getAccount(account);
        if (accountItem != null) {
            accountResource = accountItem.getConnection().getConfiguration().getResource();
        }

        PresenceManager.sortPresencesByPriority(allAccountPresences);
        int thisDeviceIndex = processThisDevicePresence(allAccountPresences, resourcesList, accountResource);
        for (int i = 0; i < allAccountPresences.size(); i++) {
            if (i == thisDeviceIndex) continue;
            processSinglePresence(allAccountPresences.get(i), resourcesList, false);
        }
    }

    private int processThisDevicePresence(List<Presence> accountPresences, List<View> resourcesList, Resourcepart deviceResource) {
        for (int i = 0; i < accountPresences.size(); i++) {
            Resourcepart accountResource = accountPresences.get(i).getFrom().getResourceOrNull();
            if (accountResource != null && accountResource.equals(deviceResource)) {
                processSinglePresence(accountPresences.get(i), resourcesList, true);
                return i;
            }
        }
        return -1;
    }

    private void processSinglePresence(Presence presence, List<View> resourcesList, boolean isThisDevice) {
        Jid fromJid = presence.getFrom();

        String client = getClientBuild(account, fromJid);


        int priorityValue = presence.getPriority();
        String priorityString = getPriorityString(priorityValue);

        Resourcepart resourceOrNull = fromJid.getResourceOrNull();
        String resource = getString(R.string.account_resource) + ": " + resourceOrNull;

        final StatusMode statusMode = StatusMode.createStatusMode(presence);

        String status = presence.getStatus();
        if (TextUtils.isEmpty(status)) {
            status = getString(statusMode.getStringID());
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View resourceView = inflater.inflate(R.layout.item_contact_info, xmppItems, false);

        resourceView.findViewById(R.id.contact_info_item_secondary);

        ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary)).setText(client);
        ((TextView)resourceView.findViewById(R.id.contact_info_item_main)).setText(status);

        ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary_second_line)).setText(resource);
        resourceView.findViewById(R.id.contact_info_item_secondary_second_line).setVisibility(View.VISIBLE);

        ((TextView)resourceView.findViewById(R.id.contact_info_item_secondary_third_line)).setText(priorityString);
        resourceView.findViewById(R.id.contact_info_item_secondary_third_line).setVisibility(View.VISIBLE);

        if (isThisDevice) {
            TextView thisDeviceIndicatorTextView
                    = (TextView) resourceView.findViewById(R.id.contact_info_item_secondary_forth_line);

            thisDeviceIndicatorTextView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(account));
            thisDeviceIndicatorTextView.setText(R.string.contact_viewer_this_device);
            thisDeviceIndicatorTextView.setVisibility(View.VISIBLE);
            resourceView.setPadding(resourceView.getPaddingLeft(),
                    Utils.dipToPx(8f, this),
                    resourceView.getPaddingRight(),
                    resourceView.getPaddingBottom());
        }

        ImageView statusIcon = (ImageView) resourceView.findViewById(R.id.contact_info_right_icon);
        statusIcon.setVisibility(View.VISIBLE);
        statusIcon.setImageResource(R.drawable.ic_status);
        statusIcon.setImageLevel(statusMode.getStatusLevel());

        resourcesList.add(resourceView);
    }

    private String getClientBuild(AccountJid account, Jid fromJid) {
        ClientInfo clientInfo = CapabilitiesManager.getInstance().getCachedClientInfo(fromJid);

        String client = "";
        if (clientInfo == null) {
            client = getString(R.string.please_wait);
            CapabilitiesManager.getInstance().requestClientInfoByUser(account, fromJid);
        } else if (clientInfo == ClientInfo.INVALID_CLIENT_INFO) {
            client = getString(R.string.unknown);
        } else {
            String name = clientInfo.getName();
            if (name != null) {
                client = name;
            }

            String type = clientInfo.getType();
            if (type != null) {
                if (client.isEmpty()) {
                    client = type;
                } else {
                    client = client + "/" + type;
                }
            }
        }
        if (!client.isEmpty()) {
            client = getString(R.string.contact_viewer_client) + ": " + client;
        }
        return client;
    }

    private String getPriorityString(int priorityValue) {
        String priorityString;
        if (priorityValue == Integer.MIN_VALUE) {
            priorityString = getString(R.string.account_priority) + ": " + getString(R.string.unknown);
        } else {
            priorityString = getString(R.string.account_priority) + ": " + priorityValue;
        }
        return priorityString;
    }

    private void addItemGroup(List<View> nameList, LinearLayout itemList, int groupIcon, boolean addSeparator) {
        if (nameList.isEmpty()) {
            return;
        }

        if (addSeparator) {
            addSeparator(itemList);
        }

        ((ImageView) nameList.get(0).findViewById(R.id.contact_info_group_icon)).setImageResource(groupIcon);

        for (View view : nameList) {
            itemList.addView(view);
        }
    }

    private void addSeparator(LinearLayout rootView) {
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        rootView.addView(inflater.inflate(R.layout.contact_info_separator, rootView, false));
    }

    @Override
    public void onAccountsChanged(@Nullable Collection<? extends AccountJid> accounts) {
        Application.getInstance().runOnUiThread(() -> {
            if (accounts != null && accounts.size() > 0 && accounts.contains(account)) {
                showOnlineAccounts();
            }
        });
    }

}
