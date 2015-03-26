package com.xabber.android.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;
import com.xabber.androiddev.R;

import java.util.Collection;

public class ContactAddFragment extends GroupEditorFragment implements AdapterView.OnItemSelectedListener {

    private static final String SAVED_NAME = "com.xabber.android.ui.ContactAdd.SAVED_NAME";
    private static final String SAVED_ACCOUNT = "com.xabber.android.ui.ContactAdd.SAVED_ACCOUNT";
    private static final String SAVED_USER = "com.xabber.android.ui.ContactAdd.SAVED_USER";


    private Spinner accountView;
    private EditText userView;
    private EditText nameView;
    private View accountSpecificPanel;

    Listener listenerActivity;

    public static ContactAddFragment newInstance(String account, String user) {
        ContactAddFragment fragment = new ContactAddFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT, account);
        args.putString(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listenerActivity = (Listener)activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View headerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.contact_add_header, null, false);
        getListView().addHeaderView(headerView);


        accountView = (Spinner) headerView.findViewById(R.id.contact_account);
        accountView.setAdapter(new AccountChooseAdapter(getActivity()));
        accountView.setOnItemSelectedListener(this);

        userView = (EditText) headerView.findViewById(R.id.contact_user);
        nameView = (EditText) headerView.findViewById(R.id.contact_name);

        accountSpecificPanel = headerView.findViewById(R.id.account_specific_panel);

        hideAdditionalInputs();

        String name;

        if (savedInstanceState != null) {
            name = savedInstanceState.getString(SAVED_NAME);
            setAccount(savedInstanceState.getString(SAVED_ACCOUNT));
            setUser(savedInstanceState.getString(SAVED_USER));
        } else {
            if (getAccount() == null || getUser() == null) {
                name = null;
            } else {
                name = RosterManager.getInstance().getName(getAccount(), getUser());
                if (getUser().equals(name)) {
                    name = null;
                }
            }
        }
        if (getAccount() == null) {
            Collection<String> accounts = AccountManager.getInstance().getAccounts();
            if (accounts.size() == 1) {
                setAccount(accounts.iterator().next());
            }
        }
        if (getAccount() != null) {
            for (int position = 0; position < accountView.getCount(); position++) {
                if (getAccount().equals(accountView.getItemAtPosition(position))) {
                    accountView.setSelection(position);
                    break;
                }
            }
        }
        if (getUser() != null) {
            userView.setText(getUser());
        }
        if (name != null) {
            nameView.setText(name);
        }

    }

    private void hideAdditionalInputs() {
        accountSpecificPanel.setVisibility(View.GONE);
        getFooterView().setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_ACCOUNT, getAccount());
        outState.putString(SAVED_USER, userView.getText().toString());
        outState.putString(SAVED_NAME, nameView.getText().toString());

    }

    @Override
    public void onDetach() {
        super.onDetach();
        listenerActivity = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedAccount = (String) accountView.getSelectedItem();

        if (selectedAccount == null) {
            onNothingSelected(parent);
            setAccount(selectedAccount);
        } else {
            listenerActivity.onAccountSelected(selectedAccount);

            if (!selectedAccount.equals(getAccount())) {
                setAccount(selectedAccount);
                setAccountGroups();
                updateGroups();
            }
            setAdditionalPanelVisibility(View.VISIBLE);
        }
    }

    private void setAdditionalPanelVisibility(int visible) {
        getFooterView().setVisibility(visible);
        accountSpecificPanel.setVisibility(visible);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        setAdditionalPanelVisibility(View.GONE);
    }

    public void addContact() {
        String user = userView.getText().toString();
        if ("".equals(user)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_USER_NAME),
                    Toast.LENGTH_LONG).show();
            return;
        }
        String account = (String) accountView.getSelectedItem();
        if (account == null) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ACCOUNT),
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            RosterManager.getInstance().createContact(account, user,
                    nameView.getText().toString(), getSelected());
            PresenceManager.getInstance().requestSubscription(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
            getActivity().finish();
            return;
        }
        MessageManager.getInstance().openChat(account, user);
        getActivity().finish();
    }

    public interface Listener {
        void onAccountSelected(String account);
    }
}
