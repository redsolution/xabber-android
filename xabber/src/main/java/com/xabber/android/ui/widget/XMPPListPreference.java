package com.xabber.android.ui.widget;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.ui.OnAddAccountClickListener;
import com.xabber.android.ui.OnReorderClickListener;
import com.xabber.android.ui.adapter.AccountListPreferenceAdapter;
import com.xabber.android.ui.preferences.PreferenceEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 29.08.17.
 */

public class XMPPListPreference extends Preference implements View.OnClickListener {

    private AccountListPreferenceAdapter accountListAdapter;
    private RelativeLayout rlReorder;
    private Button btnAddAccount;
    private PreferenceEditor activity;

    public XMPPListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setActivity(PreferenceEditor activity) {
        this.activity = activity;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View view = li.inflate( R.layout.preference_account_list, parent, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.account_list_recycler_view);

        accountListAdapter = new AccountListPreferenceAdapter(activity, activity);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(accountListAdapter);

        rlReorder = (RelativeLayout) view.findViewById(R.id.rlReorder);
        rlReorder.setOnClickListener(this);
        btnAddAccount = (Button) view.findViewById(R.id.btnAddAccount);
        btnAddAccount.setOnClickListener(this);

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

        if (accountItems.size() > 1) rlReorder.setVisibility(View.VISIBLE);
        else rlReorder.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rlReorder:
                for (OnReorderClickListener listener :
                        Application.getInstance().getUIListeners(OnReorderClickListener.class)){
                    listener.onReorderCLick();
                }
                break;
            case R.id.btnAddAccount:
                for (OnAddAccountClickListener listener :
                        Application.getInstance().getUIListeners(OnAddAccountClickListener.class)){
                    listener.onAddAccountClick();
                }
                break;
        }
    }

}
