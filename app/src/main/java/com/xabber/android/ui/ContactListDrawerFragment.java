package com.xabber.android.ui;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.xabber.android.data.Application;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.ui.adapter.AccountListAdapter;
import com.xabber.androiddev.R;

import java.util.Collection;

public class ContactListDrawerFragment extends Fragment implements View.OnClickListener, OnAccountChangedListener, AdapterView.OnItemClickListener {

    ContactListDrawerListener listener;
    private AccountListAdapter adapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (ContactListDrawerListener) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list_drawer, container, false);


        ListView listView = (ListView) view.findViewById(R.id.drawer_account_list);
        adapter = new AccountListAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        View footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.contact_list_drawer_footer, null, false);
        listView.addFooterView(footerView);

        footerView.findViewById(R.id.drawer_action_settings).setOnClickListener(this);
        footerView.findViewById(R.id.drawer_action_about).setOnClickListener(this);
        footerView.findViewById(R.id.drawer_action_exit).setOnClickListener(this);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        adapter.onChange();
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onClick(View v) {
        listener.onContactListDrawerListener(v.getId());
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        adapter.onChange();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        listener.onAccountSelected(adapter.getItem(position));
    }

    interface ContactListDrawerListener {
        void onContactListDrawerListener(int viewId);

        void onAccountSelected(String account);
    }
}
