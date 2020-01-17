package com.xabber.android.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.StatusMode;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.capability.CapabilitiesManager;
import com.xabber.android.data.extension.capability.ClientInfo;
import com.xabber.android.data.extension.xtoken.SessionVO;
import com.xabber.android.data.extension.xtoken.XTokenManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.color.ColorManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ActiveSessionsActivity extends ManagedActivity implements SessionAdapter.Listener, OnContactChangedListener, OnAccountChangedListener {

    private Toolbar toolbar;
    private BarPainter barPainter;
    private SessionAdapter adapter;
    private LinearLayout xmppItems;
    private TextView tvCurrentClient;
    private TextView tvCurrentDevice;
    private TextView tvCurrentIPAddress;
    private TextView tvCurrentDate;
    private TextView tvActiveSessions;
    private View terminateAll;
    private ProgressBar progressBar;
    private View contentView;

    private boolean XTokenEnabled = false;

    private AccountItem accountItem;

    public static Intent createIntent(Context context, AccountJid account) {
        return new AccountIntentBuilder(context, ActiveSessionsActivity.class).setAccount(account).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_sessions);

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

        if (accountItem.getConnectionSettings().getXToken() != null &&
                !accountItem.getConnectionSettings().getXToken().isExpired()) {
            XTokenEnabled = true;
        } else {
            XTokenEnabled = false;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setTitle(R.string.account_active_sessions);

        barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);
        progressBar = findViewById(R.id.progressBar);
        contentView = findViewById(R.id.contentView);
        tvActiveSessions = findViewById(R.id.tvActiveSessions);
        terminateAll = findViewById(R.id.llTerminateAll);
        terminateAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTerminateAllSessionsDialog();
            }
        });
        xmppItems = (LinearLayout) findViewById(R.id.xmpp_items);

        // other sessions
        if (XTokenEnabled) {
            RecyclerView recyclerView = findViewById(R.id.rvSessions);
            adapter = new SessionAdapter(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            recyclerView.setNestedScrollingEnabled(false);

            // current session
            tvCurrentClient = findViewById(R.id.tvClient);
            tvCurrentDevice = findViewById(R.id.tvDevice);
            tvCurrentIPAddress = findViewById(R.id.tvIPAddress);
            tvCurrentDate = findViewById(R.id.tvDate);

            getSessionsData();
        }
        //xmppItems.removeAllViews();
        showOnlineAccounts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);

    }

    private void showOnlineAccounts() {
        List<View> resourcesList = new ArrayList<>();
        fillResourceList(accountItem.getAccount(), resourcesList);
        if (!resourcesList.isEmpty()) {
            addHeader(xmppItems, getString(R.string.contact_info_connected_clients_header));
        }
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

    private void fillResourceList(AccountJid account, List<View> resourcesList) {
        final List<Presence> allPresences = RosterManager.getInstance().getPresences(account, account.getFullJid().asBareJid());

        boolean isAccount = true;
        Resourcepart accountResource = null;
        if (isAccount) {
            // TODO: probably not the best way to get own resource
            AccountItem accountItem = AccountManager.getInstance().getAccount(account);
            if (accountItem != null) {
                accountResource = accountItem.getConnection().getConfiguration().getResource();
            }
        }

        PresenceManager.sortPresencesByPriority(allPresences);

        for (Presence presence : allPresences) {
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

            if (isAccount &&resourceOrNull != null
                    && resourceOrNull.equals(accountResource)) {
                TextView thisDeviceIndicatorTextView
                        = (TextView) resourceView.findViewById(R.id.contact_info_item_secondary_forth_line);

                thisDeviceIndicatorTextView.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountSendButtonColor(account));
                thisDeviceIndicatorTextView.setText(R.string.contact_viewer_this_device);
                thisDeviceIndicatorTextView.setVisibility(View.VISIBLE);
            }

            ImageView statusIcon = (ImageView) resourceView.findViewById(R.id.contact_info_right_icon);
            statusIcon.setVisibility(View.VISIBLE);
            statusIcon.setImageResource(R.drawable.ic_status);
            statusIcon.setImageLevel(statusMode.getStatusLevel());

            resourcesList.add(resourceView);
        }
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

    private void refreshData(){
        XTokenManager.getInstance().requestSessions(
                accountItem.getConnectionSettings().getXToken().getUid(),
                accountItem.getConnection(), new XTokenManager.SessionsListener() {
                    @Override
                    public void onResult(SessionVO currentSession, List<SessionVO> sessions) {
                        setCurrentSession(currentSession);
                        adapter.setItems(sessions);
                        adapter.notifyDataSetChanged();
                        terminateAll.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                        tvActiveSessions.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(ActiveSessionsActivity.this,
                                R.string.account_active_sessions_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewMessageEvent(XTokenManager.SessionsUpdateEvent sessionsUpdateEvent) { if (XTokenEnabled) refreshData(); }

    private void getSessionsData() {
        progressBar.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.GONE);
        XTokenManager.getInstance().requestSessions(
            accountItem.getConnectionSettings().getXToken().getUid(),
            accountItem.getConnection(), new XTokenManager.SessionsListener() {
                @Override
                public void onResult(SessionVO currentSession, List<SessionVO> sessions) {
                    progressBar.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                    setCurrentSession(currentSession);
                    adapter.setItems(sessions);
                    terminateAll.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                    tvActiveSessions.setVisibility(sessions.isEmpty() ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onError() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ActiveSessionsActivity.this,
                            R.string.account_active_sessions_error, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void setCurrentSession(SessionVO session) {
        tvCurrentClient.setText(session.getClient());
        tvCurrentDevice.setText(session.getDevice());
        tvCurrentIPAddress.setText(session.getIp());
        tvCurrentDate.setText(session.getLastAuth());
    }

    @Override
    public void onItemClick(String tokenUID) {
        showTerminateSessionDialog(tokenUID);
    }

    private void showTerminateAllSessionsDialog() {
        new AlertDialog.Builder(ActiveSessionsActivity.this)
            .setMessage(R.string.terminate_all_sessions_title)
            .setPositiveButton(R.string.button_terminate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    XTokenManager.getInstance().sendRevokeXTokenRequest(
                            accountItem.getConnection(), adapter.getItemsIDs());
                    getSessionsData();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            })
            .create().show();
    }

    private void showTerminateSessionDialog(final String uid) {
        new AlertDialog.Builder(ActiveSessionsActivity.this)
            .setMessage(R.string.terminate_session_title)
            .setPositiveButton(R.string.button_terminate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    XTokenManager.getInstance().sendRevokeXTokenRequest(
                            accountItem.getConnection(), uid);
                    getSessionsData();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            })
            .create().show();
    }

    @Override
    public void onAuthErrorEvent(AccountErrorEvent accountErrorEvent) {
        super.onAuthErrorEvent(accountErrorEvent);
        finish();
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
    }
}
