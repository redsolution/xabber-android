package com.xabber.android.ui.widget;

import android.content.Context;
import android.preference.Preference;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.adapter.AccountListPreferenceAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by valery.miller on 29.08.17.
 */

public class XMPPListPreference extends Preference implements OnAccountChangedListener {

    private AccountListPreferenceAdapter accountListAdapter;

    public XMPPListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View view = li.inflate( R.layout.preference_account_list, parent, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.account_list_recycler_view);

        accountListAdapter = new AccountListPreferenceAdapter();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(accountListAdapter);

        return view;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        update();
    }

    private void update() {
        List<AccountItem> accountItems = new ArrayList<>();
        for (AccountItem accountItem : AccountManager.getInstance().getAllAccountItems()) {
            accountItems.add(accountItem);
        }
        accountListAdapter.setAccountItems(accountItems);
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        update();
    }
}
