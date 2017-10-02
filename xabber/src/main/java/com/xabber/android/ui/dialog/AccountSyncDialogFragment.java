package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;
import com.xabber.android.ui.adapter.XMPPAccountAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by valery.miller on 02.08.17.
 */

public class AccountSyncDialogFragment extends DialogFragment {

    private List<XMPPAccountSettings> xmppAccounts;
    private Switch switchSyncAll;
    private XMPPAccountAdapter adapter;
    private boolean noCancel = false;

    private static final String NO_CANCEL = "NO_CANCEL";

    public static AccountSyncDialogFragment newInstance(boolean noCancel) {
        AccountSyncDialogFragment fragment = new AccountSyncDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(NO_CANCEL, noCancel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle args = getArguments();
        boolean noCancel = args.getBoolean(NO_CANCEL, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(setupView())
                .setTitle(R.string.title_sync);


        if (!noCancel) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AccountSyncDialogFragment.this.getDialog().cancel();
                }
            }).setPositiveButton(R.string.button_sync, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onPositiveClick(false);
                }
            });
        } else {
            builder.setPositiveButton(R.string.button_sync, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onPositiveClick(true);
                }
            });
        }
        this.noCancel = noCancel;

        return builder.create();
    }

    public View setupView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_account_sync, null);

        switchSyncAll = (Switch) view.findViewById(R.id.switchSyncAll);
        switchSyncAll.setChecked(SettingsManager.isSyncAllAccounts());
        switchSyncAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (adapter != null)
                    adapter.setAllChecked(b);
            }
        });
        if (AccountManager.getInstance().haveNotAllowedSyncAccounts()) switchSyncAll.setEnabled(false);

        setXmppAccounts(XabberAccountManager.getInstance().getXmppAccountsForSync());

        adapter = new XMPPAccountAdapter(getActivity());
        adapter.setItems(xmppAccounts);

        if (adapter != null && SettingsManager.isSyncAllAccounts())
            adapter.setAllChecked(true);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    public void setXmppAccounts(List<XMPPAccountSettings> items) {
        this.xmppAccounts = new ArrayList<>();
        for (XMPPAccountSettings account : items) {
            XMPPAccountSettings newAccount = new XMPPAccountSettings(
                    account.getJid(),
                    account.isSynchronization(),
                    account.getTimestamp());
            newAccount.setUsername(account.getUsername());
            newAccount.setColor(account.getColor());
            newAccount.setOrder(account.getOrder());
            newAccount.setStatus(account.getStatus());
            newAccount.setDeleted(account.isDeleted());
            newAccount.setSyncNotAllowed(account.isSyncNotAllowed());

            this.xmppAccounts.add(newAccount);
        }
        Collections.sort(xmppAccounts);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (noCancel) ((XabberAccountInfoActivity)getActivity()).onSyncClick(true);
    }

    private void onPositiveClick(boolean needGoToMainActivity) {
        // set accounts to sync map
        if (!switchSyncAll.isChecked()) {
            Map<String, Boolean> syncState = new HashMap<>();
            for (XMPPAccountSettings account : xmppAccounts) {
                if (account.getStatus() != XMPPAccountSettings.SyncStatus.local) {
                    syncState.put(account.getJid(), account.isSynchronization());
                } else if (account.isSynchronization() || XabberAccountManager.getInstance()
                        .getAccountSyncState(account.getJid()) != null) {
                    syncState.put(account.getJid(), account.isSynchronization());
                }
            }
            XabberAccountManager.getInstance().setAccountSyncState(syncState);
        }

        // update timestamp in accounts if timestamp was changed in dialog
        for (XMPPAccountSettings account : xmppAccounts) {
            AccountJid accountJid = XabberAccountManager.getInstance().getExistingAccount(account.getJid());
            if (accountJid != null)
                AccountManager.getInstance().setTimestamp(accountJid, account.getTimestamp());
        }

        // set sync all
        SettingsManager.setSyncAllAccounts(switchSyncAll.isChecked());

        // callback to sync-request
        ((XabberAccountInfoActivity)getActivity()).onSyncClick(needGoToMainActivity);
    }
}
