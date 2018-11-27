package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.ui.color.ColorManager;

/**
 * Created by valery.miller on 19.09.17.
 */

public class EnterPassDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARGUMENT_JID = "ARGUMENT_JID";
    private static final String ARGUMENT_TOKEN = "ARGUMENT_TOKEN";
    private static final String ARGUMENT_TIMESTAMP = "ARGUMENT_TIMESTAMP";
    private static final String ARGUMENT_ORDER = "ARGUMENT_ORDER";
    private static final String ARGUMENT_COLOR = "ARGUMENT_COLOR";

    private String jid;
    private String token;
    private int timestamp;
    private int order;
    private String color;

    private EditText edtPass;

    public static DialogFragment newInstance(XMPPAccountSettings account) {
        EnterPassDialog fragment = new EnterPassDialog();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_JID, account.getJid());
        arguments.putString(ARGUMENT_TOKEN, account.getToken());
        arguments.putInt(ARGUMENT_TIMESTAMP, account.getTimestamp());
        arguments.putInt(ARGUMENT_ORDER, account.getOrder());
        arguments.putString(ARGUMENT_COLOR, account.getColor());

        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        jid = args.getString(ARGUMENT_JID);
        token = args.getString(ARGUMENT_TOKEN);
        color = args.getString(ARGUMENT_COLOR);
        order = args.getInt(ARGUMENT_ORDER);
        timestamp = args.getInt(ARGUMENT_TIMESTAMP);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(jid)
                .setView(setUpDialogView())
                .setPositiveButton(R.string.login, this)
                .setNegativeButton(R.string.skip, this);

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    @NonNull
    private View setUpDialogView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_enter_pass, null);
        edtPass = (EditText) view.findViewById(R.id.edtPass);

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE)
            createAccount(true);
        if (which == Dialog.BUTTON_NEGATIVE)
            createAccount(false);
    }

    private void createAccount(boolean enabled) {
        try {
            AccountJid accountJid = AccountManager.getInstance().addAccount(jid,
                    edtPass.getText().toString(), token, false, true,
                    true, false, false, enabled, false);
            AccountManager.getInstance().setColor(accountJid, ColorManager.getInstance().convertColorNameToIndex(color));
            AccountManager.getInstance().setOrder(accountJid, order);
            AccountManager.getInstance().setTimestamp(accountJid, timestamp);
            AccountManager.getInstance().onAccountChanged(accountJid);
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
        }
    }
}
