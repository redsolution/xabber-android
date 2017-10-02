package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.ui.activity.ChatActivity;
import com.xabber.android.ui.activity.ConferenceAddActivity;
import com.xabber.android.ui.activity.ConferenceFilterActivity;
import com.xabber.android.ui.adapter.AccountChooseAdapter;
import com.xabber.android.ui.adapter.HostedConferencesAdapter;
import com.xabber.android.ui.color.ColorManager;

import org.jivesoftware.smackx.muc.HostedRoom;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConferenceSelectFragment extends ListFragment implements AdapterView.OnItemSelectedListener,
        View.OnClickListener, MUCManager.HostedRoomsListener, AdapterView.OnItemClickListener {

    private static final String LOG_TAG = ConferenceSelectFragment.class.getSimpleName();
    private Spinner accountView;
    private EditText serverView;
    private EditText roomView;
    private HostedConferencesAdapter hostedConferencesAdapter;
    private View roomsProgressBar;
    private AccountJid account;

    Listener listener;
    private Button nextButton;

    public ConferenceSelectFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        listener = (Listener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conference_select, container, false);

        accountView = (Spinner) view.findViewById(R.id.contact_account);
        serverView = (EditText) view.findViewById(R.id.muc_server);
        roomView = (EditText) view.findViewById(R.id.muc_conference_name);

        roomView.setOnClickListener(this);
        roomView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    onRoomNameEditTextClick();
                }
            }
        });

        roomsProgressBar = view.findViewById(R.id.muc_rooms_progress_bar);

        view.findViewById(R.id.muc_get_hosted_rooms).setOnClickListener(this);

        accountView.setAdapter(new AccountChooseAdapter(getActivity()));
        accountView.setOnItemSelectedListener(this);

        if (AccountManager.getInstance().getEnabledAccounts().size() == 1) {
            accountView.setSelection(0);
        }

        nextButton = (Button) view.findViewById(R.id.muc_next);
        nextButton.setTextColor(ColorManager.getInstance().getAccountPainter().getDefaultTextColor());
        nextButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        hostedConferencesAdapter = new HostedConferencesAdapter(getActivity());

        ListView listView = getListView();
        listView.setAdapter(hostedConferencesAdapter);
        listView.setOnItemClickListener(this);


        if (savedInstanceState != null) {
            hostedConferencesAdapter.clear();
            hostedConferencesAdapter.addAll(ConferenceFilterActivity.restoreConferenceList(savedInstanceState));
        }

        roomView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hostedConferencesAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        storeConferenceList(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (account == null) {
            LogManager.w(LOG_TAG, "onItemClick: Account is null!");
            return;
        }

        try {
            startActivity(ConferenceAddActivity.createIntent(getActivity(), account,
                    UserJid.from(hostedConferencesAdapter.getItem(position).getJid())));
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        AccountJid newAccount = (AccountJid) accountView.getSelectedItem();

        if (account != null && account.equals(newAccount)) {
            return;
        }

        if (account != null) {
            hostedConferencesAdapter.clear();
        }

        account = newAccount;
        listener.onAccountSelected(account);

        nextButton.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountTextColor(account));

        serverView.setText(getString(R.string.domen_part, account.getFullJid().getDomain().toString()));
        onRequestHostedRoomsClick();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        account = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.muc_get_hosted_rooms:
                onRequestHostedRoomsClick();
                break;
            case R.id.muc_next:
                onNextClick();
                break;

            case R.id.muc_conference_name:
                onRoomNameEditTextClick();
                break;

        }
    }

    private void onRoomNameEditTextClick() {
        if (hostedConferencesAdapter.isEmpty()) {
            return;
        }

        Intent intent = ConferenceFilterActivity.createIntent(getActivity(), account);
        intent.putExtra(ConferenceFilterActivity.ARG_CONFERENCE_NAME, roomView.getText().toString());


        Bundle bundle = new Bundle();
        storeConferenceList(bundle);
        intent.putExtra(ConferenceFilterActivity.ARG_CONFERENCE_LIST, bundle);

        startActivityForResult(intent, ConferenceFilterActivity.REQUEST_CODE_FILTER_ROOMS);
    }

    private void storeConferenceList(Bundle intent) {
        List<HostedRoom> conferencesList = hostedConferencesAdapter.getConferencesList();

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> jids = new ArrayList<>();

        for (HostedRoom hostedRoom : conferencesList) {
            names.add(hostedRoom.getName());
            jids.add(hostedRoom.getJid().toString());
        }

        intent.putStringArrayList(ConferenceFilterActivity.ARG_CONFERENCE_LIST_NAMES, names);
        intent.putStringArrayList(ConferenceFilterActivity.ARG_CONFERENCE_LIST_JIDS, jids);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ConferenceFilterActivity.REQUEST_CODE_FILTER_ROOMS) {
            String stringExtra = data.getStringExtra(ConferenceFilterActivity.ARG_CONFERENCE_NAME);
            if (stringExtra != null) {
                onConferenceNameChanged(stringExtra);
            }
        }
    }

    private void onNextClick() {
        if (account == null) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ACCOUNT), Toast.LENGTH_SHORT).show();
            return;
        }
        DomainBareJid server;
        try {
            server = JidCreate.domainBareFrom(serverView.getText().toString());
        } catch (XmppStringprepException e) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_SERVER_NAME), Toast.LENGTH_SHORT).show();
            return;
        }

        Localpart room;
        try {
            room = Localpart.from(roomView.getText().toString());
        } catch (XmppStringprepException e) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ROOM_NAME), Toast.LENGTH_LONG).show();
            return;
        }

        UserJid roomJid = null;
        try {
            roomJid = UserJid.from(JidCreate.entityBareFrom(room, server));
            startActivity(ConferenceAddActivity.createIntent(getActivity(), account, roomJid));
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
        }
    }

    private void onRequestHostedRoomsClick() {
        if (account == null) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ACCOUNT), Toast.LENGTH_SHORT).show();
            return;
        }
        DomainBareJid server;
        try {
            server = JidCreate.domainBareFrom(serverView.getText().toString());
        } catch (XmppStringprepException e) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_SERVER_NAME), Toast.LENGTH_SHORT).show();
            return;
        }

        ChatActivity.hideKeyboard(getActivity());

        hostedConferencesAdapter.clear();
        roomsProgressBar.setVisibility(View.VISIBLE);
        MUCManager.requestHostedRooms(account, server, this);
    }

    @Override
    public void onHostedRoomsReceived(Collection<HostedRoom> hostedRooms) {
        if (!isAdded()) {
            return;
        }

        roomsProgressBar.setVisibility(View.GONE);

        if (hostedRooms == null) {
            Toast.makeText(getActivity(), R.string.muc_error_getting_conferences, Toast.LENGTH_SHORT).show();
            return;
        }

        hostedConferencesAdapter.clear();
        hostedConferencesAdapter.addAll(hostedRooms);
    }

    public void onConferenceNameChanged(String stringExtra) {
        roomView.setText(stringExtra);
        roomView.setSelection(stringExtra.length());
    }

    public interface Listener {
        void onAccountSelected(AccountJid account);
    }
}
