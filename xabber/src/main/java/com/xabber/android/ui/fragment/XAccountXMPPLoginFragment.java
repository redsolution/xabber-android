package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.adapter.XMPPAccountAuthAdapter;

import java.util.ArrayList;

public class XAccountXMPPLoginFragment extends Fragment implements XMPPAccountAuthAdapter.Listener {

    private View progressView;
    private XMPPAccountAuthAdapter adapter;

    private Listener listener;

    public interface Listener {
        void onAccountClick(String jid);
    }

    public static XAccountXMPPLoginFragment newInstance() {
        XAccountXMPPLoginFragment fragment = new XAccountXMPPLoginFragment();
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
        progressView = view.findViewById(R.id.progressView);

        RecyclerView recyclerView = view.findViewById(R.id.rlAccounts);
        adapter = new XMPPAccountAuthAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
        else throw new RuntimeException(context.toString()
                + " must implement XAccountXMPPLoginFragment.Listener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onAccountClick(String accountJid) {
        listener.onAccountClick(accountJid);
    }

    private void loadData() {
        ArrayList<AccountJid> accounts = new ArrayList<>(AccountManager.getInstance().getEnabledAccounts());
        if (accounts.size() < 1)
            Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
        else if (accounts.size() > 1)
            adapter.setItems(accounts);
        else listener.onAccountClick(accounts.get(0).getFullJid().toString());
    }

    private void showProgress(boolean show) {
        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
