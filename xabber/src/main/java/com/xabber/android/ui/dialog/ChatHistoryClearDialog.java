package com.xabber.android.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.rrr.RrrManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

public class ChatHistoryClearDialog extends DialogFragment implements View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = ChatHistoryClearDialog.class.getName() + "ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = ChatHistoryClearDialog.class.getName() + "ARGUMENT_USER";
    private CheckBox checkBox;

    AccountJid account;
    UserJid user;

    public static ChatHistoryClearDialog newInstance(AccountJid account, UserJid user) {
        ChatHistoryClearDialog fragment = new ChatHistoryClearDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_clear_history, null);

        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);
        String contactName = RosterManager.getInstance().getBestContact(account, user).getName();
        int colorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account);

        ((TextView) view.findViewById(R.id.clear_history_confirm)).setText(Html.fromHtml(getString(R.string.clear_chat_history_dialog_message, contactName)));
        ((TextView) view.findViewById(R.id.clear_history_warning)).setText(getString(R.string.clear_chat_history_dialog_warning));

        ((Button) view.findViewById(R.id.clear)).setTextColor(colorIndicator);

        ((Button) view.findViewById(R.id.clear)).setOnClickListener(this);
        ((Button) view.findViewById(R.id.cancel_clear)).setOnClickListener(this);

        checkBox = (CheckBox) view.findViewById(R.id.clear_history_retract);

        if (RrrManager.getInstance().isSupported(account)){
            checkBox.setVisibility(View.VISIBLE);
        }

        return builder.setTitle(R.string.clear_history)
                .setView(view)
                .create();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.clear) {
            if (RrrManager.getInstance().isSupported(account))
                RrrManager.getInstance().sendRetractAllMessagesRequest(account, user, checkBox.isChecked());
            else MessageManager.getInstance().clearHistory(account, user);
        }
        dismiss();
    }
}
