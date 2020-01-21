package com.xabber.android.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.ColorManager;

public class ChatDeleteDialog extends DialogFragment implements View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ChatDeleteDialog.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ChatDeleteDialog.ARGUMENT_USER";
    //public static final String ARGUMENT_CHAT = "com.xabber.android.ui.dialog.ChatDeleteDialog.ARGUMENT_CHAT";

    AccountJid account;
    UserJid user;

    public static ChatDeleteDialog newInstance(AccountJid account, UserJid user) {
        ChatDeleteDialog fragment = new ChatDeleteDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_delete_chat, null);

        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);
        String contactName = RosterManager.getInstance().getBestContact(account, user).getName();
        int colorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account);

        ((TextView) view.findViewById(R.id.delete_chat_confirm)).setText(getString(R.string.delete_chat_confirm, contactName));
        ((TextView) view.findViewById(R.id.delete_chat_warning)).setText(getString(R.string.delete_chat_warning));

        ((Button) view.findViewById(R.id.delete)).setTextColor(colorIndicator);

        ((Button) view.findViewById(R.id.delete)).setOnClickListener(this);
        ((Button) view.findViewById(R.id.cancel_delete)).setOnClickListener(this);

        return builder.setTitle(R.string.clear_history)
                .setView(view)
                .create();

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.delete) {
            AbstractChat chat = MessageManager.getInstance().getChat(account, user);
            if (chat != null) {
                MessageManager.getInstance().clearHistory(account, user);
                MessageManager.getInstance().removeChat(chat);
            }
        }
        dismiss();
    }
}
