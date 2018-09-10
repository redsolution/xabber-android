package com.xabber.android.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.ui.adapter.XMPPAccountAuthAdapter;

import java.util.ArrayList;
import java.util.List;

public class ChooseAccountForXMPPAuthDialog extends DialogFragment implements XMPPAccountAuthAdapter.Listener {

    private List<AccountJid> xmppAccounts = new ArrayList<>();
    private Listener listener;

    public interface Listener {
        void onAccountClick(String accountJid);
    }

    public static ChooseAccountForXMPPAuthDialog newInstance(Listener listener, ArrayList<AccountJid> items) {
        ChooseAccountForXMPPAuthDialog fragment = new ChooseAccountForXMPPAuthDialog();
        fragment.listener = listener;
        fragment.xmppAccounts = items;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(setupView())
                .setTitle(R.string.choose_account);

        return builder.create();
    }

    public View setupView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_xaccount_xmpp_auth, null);

        XMPPAccountAuthAdapter adapter = new XMPPAccountAuthAdapter(this);
        adapter.setItems(xmppAccounts);

        RecyclerView recyclerView = view.findViewById(R.id.rlAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onAccountClick(String accountJid) {
        listener.onAccountClick(accountJid);
        dismiss();
    }
}
