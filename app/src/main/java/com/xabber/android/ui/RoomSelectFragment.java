package com.xabber.android.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
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
import com.xabber.android.ui.adapter.AccountChooseAdapter;
import com.xabber.android.ui.adapter.HostedRoomsAdapter;
import com.xabber.android.ui.helper.AccountPainter;

import org.jivesoftware.smackx.muc.HostedRoom;

import java.util.Collection;

public class RoomSelectFragment extends ListFragment implements AdapterView.OnItemSelectedListener,
        View.OnClickListener, MUCManager.HostedRoomsListener, AdapterView.OnItemClickListener {

    private Spinner accountView;
    private EditText serverView;
    private EditText roomView;
    private HostedRoomsAdapter hostedRoomsAdapter;
    private View roomsProgressBar;
    private String account;

    Listener listener;
    private AccountPainter accountPainter;
    private Button nextButton;

    public RoomSelectFragment() {
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
        View view = inflater.inflate(R.layout.room_select_fragment, container, false);

        accountView = (Spinner) view.findViewById(R.id.contact_account);
        serverView = (EditText) view.findViewById(R.id.muc_server);
        roomView = (EditText) view.findViewById(R.id.muc_room);

        roomsProgressBar = view.findViewById(R.id.muc_rooms_progress_bar);

        view.findViewById(R.id.muc_get_hosted_rooms).setOnClickListener(this);

        accountView.setAdapter(new AccountChooseAdapter(getActivity()));
        accountView.setOnItemSelectedListener(this);

        if (AccountManager.getInstance().getAccounts().size() == 1) {
            accountView.setSelection(0);
        }

        accountPainter = new AccountPainter(getActivity());

        nextButton = (Button) view.findViewById(R.id.muc_next);
        nextButton.setTextColor(accountPainter.getDefaultDarkColor());
        nextButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        hostedRoomsAdapter = new HostedRoomsAdapter(getActivity(), android.R.layout.simple_list_item_2);

        ListView listView = getListView();
        listView.setAdapter(hostedRoomsAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(ConferenceAdd.createIntent(getActivity(), account,
                hostedRoomsAdapter.getItem(position).getJid()));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String newAccount = (String) accountView.getSelectedItem();

        if (account != null && account.equals(newAccount)) {
            return;
        }

        account = newAccount;
        listener.onAccountSelected(account);

        nextButton.setTextColor(accountPainter.getAccountDarkColor(account));

        hostedRoomsAdapter.clear();
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
        hostedRoomsAdapter.clear();
        roomsProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onHostedRoomsReceived(Collection<HostedRoom> hostedRooms) {
        roomsProgressBar.setVisibility(View.GONE);

        if (hostedRooms == null) {
            Toast.makeText(getActivity(), "Error getting rooms", Toast.LENGTH_SHORT).show();
            return;
        }

        hostedRoomsAdapter.clear();
        hostedRoomsAdapter.addAll(hostedRooms);
    }

    interface Listener {
        void onAccountSelected(String account);
    }
}
