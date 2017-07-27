package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;
import com.xabber.android.ui.adapter.XMPPAccountAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 27.07.17.
 */

public class XabberAccountInfoFragment extends Fragment {

    private TextView tvAccountName;
    private TextView tvAccountEmail;
    private RelativeLayout rlLogout;
    private RelativeLayout rlSync;
    private XMPPAccountAdapter adapter;
    private List<XMPPAccountSettings> xmppAccounts;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xabber_account_info, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        tvAccountEmail = (TextView) view.findViewById(R.id.tvAccountEmail);

        rlLogout = (RelativeLayout) view.findViewById(R.id.rlLogout);
        rlLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((XabberAccountInfoActivity)getActivity()).onLogoutClick();
            }
        });

        rlSync = (RelativeLayout) view.findViewById(R.id.rlSync);
        rlSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((XabberAccountInfoActivity)getActivity()).onSyncClick();
            }
        });

        adapter = new XMPPAccountAdapter();
        xmppAccounts = new ArrayList<>();
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.rcvXmppUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setNestedScrollingEnabled(false);
        adapter.setItems(xmppAccounts);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        List<XMPPAccountSettings> items = XabberAccountManager.getInstance().getXmppAccounts();
        if (items != null) updateList(items);

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) updateData(account);
        else ((XabberAccountInfoActivity)getActivity()).showLoginFragment();
    }

    public void updateData(@NonNull XabberAccount account) {
        String accountName = account.getFirstName() + " " + account.getLastName();
        tvAccountName.setText(accountName);
        tvAccountEmail.setText(account.getEmails().get(0).getEmail());
    }

    public void updateList(@NonNull List<XMPPAccountSettings> list) {
        xmppAccounts.clear();
        xmppAccounts.addAll(list);
        adapter.setItems(xmppAccounts);
    }

}
