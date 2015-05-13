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

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.ui.adapter.NavigationDrawerAccountAdapter;
import com.xabber.android.ui.helper.AccountPainter;

import java.util.Collection;

public class ContactListDrawerFragment extends Fragment implements View.OnClickListener, OnAccountChangedListener, AdapterView.OnItemClickListener {

    ContactListDrawerListener listener;
    private NavigationDrawerAccountAdapter adapter;
    private ListView listView;
    private View divider;
    private View headerTitle;
    private View drawerHeader;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (ContactListDrawerListener) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list_drawer, container, false);

        drawerHeader = view.findViewById(R.id.drawer_header);

        listView = (ListView) view.findViewById(R.id.drawer_account_list);

        View footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.contact_list_drawer_footer, listView, false);
        listView.addFooterView(footerView);

        View headerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.contact_list_drawer_header, listView, false);
        headerTitle = headerView.findViewById(R.id.drawer_header_title);


        listView.addHeaderView(headerView);

        adapter = new NavigationDrawerAccountAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        footerView.findViewById(R.id.drawer_action_settings).setOnClickListener(this);
        footerView.findViewById(R.id.drawer_action_about).setOnClickListener(this);
        footerView.findViewById(R.id.drawer_action_exit).setOnClickListener(this);

        divider = footerView.findViewById(R.id.drawer_divider);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        update();
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
        update();
    }

    private void update() {
        adapter.onChange();
        drawerHeader.getBackground().setLevel(AccountPainter.getDefaultAccountColorLevel());

        if (adapter.getCount() == 0) {
            headerTitle.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        } else {
            headerTitle.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        listener.onAccountSelected((String) listView.getItemAtPosition(position));
    }

    interface ContactListDrawerListener {
        void onContactListDrawerListener(int viewId);

        void onAccountSelected(String account);
    }
}
