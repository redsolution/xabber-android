package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.XMPPAccountAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountSyncFragment extends Fragment {

    private List<XMPPAccountSettings> xmppAccounts;
    private Switch switchSyncAll;
    private XMPPAccountAdapter adapter;

    public static AccountSyncFragment newInstance() {
        AccountSyncFragment fragment = new AccountSyncFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_account_sync, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    }

    private void setXmppAccounts(List<XMPPAccountSettings> items) {
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
}
