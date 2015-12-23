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
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.ui.activity.ChatViewer;
import com.xabber.android.ui.activity.ConferenceAdd;
import com.xabber.android.ui.activity.ConferenceFilterActivity;
import com.xabber.android.ui.adapter.AccountChooseAdapter;
import com.xabber.android.ui.adapter.HostedConferencesAdapter;
import com.xabber.android.ui.color.ColorManager;

import org.jivesoftware.smackx.muc.HostedRoom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConferenceSelectFragment extends ListFragment implements AdapterView.OnItemSelectedListener,
        View.OnClickListener, MUCManager.HostedRoomsListener, AdapterView.OnItemClickListener {

    private Spinner accountView;
    private EditText serverView;
    private EditText roomView;
    private HostedConferencesAdapter hostedConferencesAdapter;
    private View roomsProgressBar;
    private String account;

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
        View view = inflater.inflate(R.layout.conference_select_fragment, container, false);

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

        roomsProgressBar = view.findViewById(R.id.muc_rooms_progress_bar);

        view.findViewById(R.id.muc_get_hosted_rooms).setOnClickListener(this);

        accountView.setAdapter(new AccountChooseAdapter(getActivity()));
        accountView.setOnItemSelectedListener(this);

        if (AccountManager.getInstance().getAccounts().size() == 1) {
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


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        storeConferenceList(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(ConferenceAdd.createIntent(getActivity(), account,
                hostedConferencesAdapter.getItem(position).getJid()));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String newAccount = (String) accountView.getSelectedItem();

        if (account != null && account.equals(newAccount)) {
            return;
        }

        if (account != null) {
            hostedConferencesAdapter.clear();
        }

        account = newAccount;
        listener.onAccountSelected(account);

        nextButton.setTextColor(ColorManager.getInstance().getAccountPainter().getAccountTextColor(account));


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
        List<HostedRoom> conferencesList = new ArrayList<>();
        conferencesList.addAll(hostedConferencesAdapter.getConferencesList());

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> jids = new ArrayList<>();

        for (HostedRoom hostedRoom : conferencesList) {
            names.add(hostedRoom.getName());
            jids.add(hostedRoom.getJid());
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
        String server = serverView.getText().toString();
        if ("".equals(server)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_SERVER_NAME), Toast.LENGTH_SHORT).show();
            return;
        }

        String room = roomView.getText().toString();
        if ("".equals(room)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ROOM_NAME), Toast.LENGTH_LONG).show();
            return;
        }

        room = room + "@" + server;

        startActivity(ConferenceAdd.createIntent(getActivity(), account, room));
    }

    private void onRequestHostedRoomsClick() {
        if (account == null) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_ACCOUNT), Toast.LENGTH_SHORT).show();
            return;
        }
        String server = serverView.getText().toString();
        if ("".equals(server)) {
            Toast.makeText(getActivity(), getString(R.string.EMPTY_SERVER_NAME), Toast.LENGTH_SHORT).show();
            return;
        }

        ChatViewer.hideKeyboard(getActivity());
        MUCManager.requestHostedRooms(account, server, this);
        hostedConferencesAdapter.clear();
        roomsProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onHostedRoomsReceived(Collection<HostedRoom> hostedRooms) {
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
        void onAccountSelected(String account);
    }
}
