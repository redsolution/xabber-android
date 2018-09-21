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
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.privatestorage.PrivateStorageManager;
import com.xabber.android.ui.adapter.XMPPAccountAuthAdapter;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class XAccountXMPPLoginFragment extends Fragment implements XMPPAccountAuthAdapter.Listener {

    private View progressView;
    private TextView tvProgressTitle;
    private TextView tvError;
    private XMPPAccountAuthAdapter adapter;
    private RecyclerView recyclerView;

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
        tvProgressTitle = view.findViewById(R.id.tvProgressTitle);
        tvError = view.findViewById(R.id.tvError);
        recyclerView = view.findViewById(R.id.rlAccounts);
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
        tvError.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        ArrayList<AccountJid> accounts = new ArrayList<>(AccountManager.getInstance().getEnabledAccounts());
        if (accounts.size() < 1) tvError.setVisibility(View.VISIBLE);
        else if (accounts.size() > 1) loadBindings(accounts);
        else connectViaJid(accounts.get(0).getFullJid().toString());
    }

    private void loadBindings(ArrayList<AccountJid> accounts) {
        showProgress(getActivity().getResources().getString(R.string.progress_title_account_list));
        PrivateStorageManager.getInstance().getAccountViewWithBindings(accounts)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<XMPPAccountAuthAdapter.AccountView>>() {
                @Override
                public void call(List<XMPPAccountAuthAdapter.AccountView> accountViews) {
                    hideProgress();
                    adapter.setItems(accountViews);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
    }

    private void showProgress(String title) {
        tvProgressTitle.setText(title);
        progressView.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressView.setVisibility(View.GONE);
    }

    private void connectViaJid(String jid) {
        showProgress(getActivity().getResources().getString(R.string.progress_title_connect, jid));
        listener.onAccountClick(jid);
    }
}
