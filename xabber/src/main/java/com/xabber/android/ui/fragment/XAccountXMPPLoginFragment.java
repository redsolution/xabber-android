package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.avatar.AvatarManager;
import com.xabber.android.data.extension.privatestorage.PrivateStorageManager;
import com.xabber.android.ui.adapter.XMPPAccountAuthAdapter;
import com.xabber.android.ui.color.ColorManager;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class XAccountXMPPLoginFragment extends Fragment implements XMPPAccountAuthAdapter.Listener {

    private View progressView;
    private TextView tvError;
    private TextView tvTitle;

    private View loginProgressView;
    private TextView tvAccountJidInLoginProgressView;
    private TextView tvActionInLoginProgressView;
    private ImageView ivAvatarInLoginProgressView;

    private XMPPAccountAuthAdapter adapter;
    private RecyclerView recyclerView;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
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
        tvTitle = view.findViewById(R.id.tvTitle);
        tvError = view.findViewById(R.id.tvError);

        loginProgressView = view.findViewById(R.id.loginProgressView);
        tvAccountJidInLoginProgressView = loginProgressView.findViewById(R.id.tvAccountJid);
        tvActionInLoginProgressView = loginProgressView.findViewById(R.id.tvAction);
        ivAvatarInLoginProgressView = loginProgressView.findViewById(R.id.avatar);

        recyclerView = view.findViewById(R.id.rlAccounts);
        adapter = new XMPPAccountAuthAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) listener = (Listener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement XAccountXMPPLoginFragment.Listener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        compositeSubscription.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onAccountClick(AccountJid accountJid) {
        connectViaJid(accountJid);
    }

    private void loadData() {
        tvError.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvTitle.setVisibility(View.GONE);

        ArrayList<AccountJid> accounts = new ArrayList<>(AccountManager.getInstance().getEnabledAccounts());
        if (accounts.size() < 1) tvError.setVisibility(View.VISIBLE);
        else if (accounts.size() > 1) loadBindings(accounts);
        else connectViaJid(accounts.get(0));
    }

    private void loadBindings(ArrayList<AccountJid> accounts) {
        showProgress(true);
        compositeSubscription.add(PrivateStorageManager.getInstance().getAccountViewWithBindings(accounts)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<XMPPAccountAuthAdapter.AccountView>>() {
                @Override
                public void call(List<XMPPAccountAuthAdapter.AccountView> accountViews) {
                    showProgress(false);
                    adapter.setItems(accountViews);
                    recyclerView.setVisibility(View.VISIBLE);
                    tvTitle.setVisibility(View.VISIBLE);
                }
            }));
    }

    private void showProgress(boolean show) {
        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showLoginProgress(AccountJid account) {

        // set color
        tvAccountJidInLoginProgressView.setTextColor(ColorManager.getInstance().getAccountPainter().
                getAccountMainColor(account));

        // set avatar
        ivAvatarInLoginProgressView.setImageDrawable(
                AvatarManager.getInstance().getAccountAvatar(account));

        // set jid
        final String accountJid = account.getFullJid().asBareJid().toString();
        tvAccountJidInLoginProgressView.setText(accountJid);

        // set action
        tvActionInLoginProgressView.setText(R.string.progress_title_connect);

        // show view
        loginProgressView.setVisibility(View.VISIBLE);
    }

    private void connectViaJid(AccountJid jid) {
        recyclerView.setVisibility(View.GONE);
        tvTitle.setVisibility(View.GONE);
        showLoginProgress(jid);
        listener.onAccountClick(jid.getFullJid().toString());
    }
}
