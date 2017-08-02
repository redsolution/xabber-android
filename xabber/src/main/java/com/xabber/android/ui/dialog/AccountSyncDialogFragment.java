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
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;
import com.xabber.android.ui.adapter.XMPPAccountAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by valery.miller on 02.08.17.
 */

public class AccountSyncDialogFragment extends DialogFragment {

    private List<XMPPAccountSettings> xmppAccounts;
    private Switch switchSyncAll;
    private XMPPAccountAdapter adapter;

    public static AccountSyncDialogFragment newInstance() {
        return new AccountSyncDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(setupView())
                .setMessage("Synchronization")
                .setPositiveButton("synchronize", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XabberAccountManager.getInstance().setSyncAllAccounts(xmppAccounts);
                        SettingsManager.setSyncAllAccounts(switchSyncAll.isChecked());
                        ((XabberAccountInfoActivity)getActivity()).onSyncClick();
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AccountSyncDialogFragment.this.getDialog().cancel();
                    }
                });
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

        setXmppAccounts(XabberAccountManager.getInstance().getXmppAccounts());

        adapter = new XMPPAccountAdapter();
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

            this.xmppAccounts.add(newAccount);
        }
        Collections.sort(xmppAccounts);
    }
}
