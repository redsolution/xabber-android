package com.xabber.android.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.AccountChooseAdapter;

import java.util.Collection;

public class ContactAddFragment extends GroupEditorFragment
        implements AdapterView.OnItemSelectedListener, ContactAdder {

    private static final String SAVED_NAME = "com.xabber.android.ui.ContactAdd.SAVED_NAME";
    private static final String SAVED_ACCOUNT = "com.xabber.android.ui.ContactAdd.SAVED_ACCOUNT";
    private static final String SAVED_USER = "com.xabber.android.ui.ContactAdd.SAVED_USER";
    Listener listenerActivity;
    private Spinner accountView;
    private EditText userView;
    private EditText nameView;
    private String name;
    private View accountSelectorPanel;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_add_fragment, container, false);


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

        accountSelectorPanel = view.findViewById(R.id.account_selector);

        setUpAccountView((Spinner) view.findViewById(R.id.contact_account));

        return view;
    }

    private void setUpAccountView(Spinner view) {
        accountView = view;
        accountView.setAdapter(new AccountChooseAdapter(getActivity()));
        accountView.setOnItemSelectedListener(this);

        if (getAccount() != null) {
            for (int position = 0; position < accountView.getCount(); position++) {
                if (getAccount().equals(accountView.getItemAtPosition(position))) {
                    accountView.setSelection(position);
                    break;
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setVisibility(View.GONE);
    }

    private void setUpListView() {
        View headerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.contact_add_header, null, false);
        getListView().addHeaderView(headerView);

        accountSelectorPanel.setVisibility(View.GONE);

        setUpAccountView((Spinner) headerView.findViewById(R.id.contact_account));

        userView = (EditText) headerView.findViewById(R.id.contact_user);
        nameView = (EditText) headerView.findViewById(R.id.contact_name);

        if (getUser() != null) {
            userView.setText(getUser());
        }
        if (name != null) {
            nameView.setText(name);
        }
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

            if (getListView().getVisibility() == View.GONE) {
                setUpListView();
                getListView().setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void addContact() {
        if (getAccount() == null) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ACCOUNT),
                    Toast.LENGTH_LONG).show();
            return;
        }

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
