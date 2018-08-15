package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.ui.adapter.XMPPAccountAuthAdapter;

import java.util.ArrayList;
import java.util.List;

public class XAccountXMPPAuthFragment extends Fragment implements XMPPAccountAuthAdapter.Listener {

    private List<AccountItem> xmppAccounts = new ArrayList<>();
    private Listener listener;

    public interface Listener {
        void onAccountClick(String accountJid);
    }

    public static XAccountXMPPAuthFragment newInstance(Listener listener) {
        XAccountXMPPAuthFragment fragment = new XAccountXMPPAuthFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_xmpp_auth, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        xmppAccounts.clear();
        xmppAccounts.addAll(AccountManager.getInstance().getAllAccountItems());

        XMPPAccountAuthAdapter adapter = new XMPPAccountAuthAdapter(this);
        adapter.setItems(xmppAccounts);

        RecyclerView recyclerView = view.findViewById(R.id.rlAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAccountClick(String accountJid) {
        listener.onAccountClick(accountJid);
    }
}
