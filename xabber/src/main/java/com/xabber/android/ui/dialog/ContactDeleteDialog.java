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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.color.ColorManager;

public class ContactDeleteDialog extends DialogFragment implements View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ContactDeleteDialog.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ContactDeleteDialog.ARGUMENT_USER";

    private ContactJid user;
    private AccountJid account;

    private CheckBox deleteHistory;

    public static ContactDeleteDialog newInstance(AccountJid account, ContactJid user) {
        ContactDeleteDialog fragment = new ContactDeleteDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_delete_contact, null);

        Bundle args = getArguments();

        if (args == null) dismiss();

        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);
        String contactName = RosterManager.getInstance().getBestContact(account, user).getName();
        int colorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account);

        ((TextView) view.findViewById(R.id.delete_contact_confirm))
                .setText(Html.fromHtml(getString(R.string.contact_delete_confirm_short,
                        contactName)));

        ((TextView) view.findViewById(R.id.delete_contact_warning))
                .setText(getString(R.string.contact_delete_warning));

        ((Button) view.findViewById(R.id.delete)).setTextColor(colorIndicator);

        deleteHistory = view.findViewById(R.id.clear_history);

        view.findViewById(R.id.cancel_delete).setOnClickListener(this);
        view.findViewById(R.id.delete).setOnClickListener(this);

        return builder.setTitle(getString(R.string.contact_delete_full))
                .setView(view)
                .create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_delete:
                dismiss();
                break;
            case R.id.delete:
                try {
                    // discard subscription
                    PresenceManager.getInstance().discardSubscription(account, user);
                    PresenceManager.getInstance().unsubscribeFromPresence(account, user);
                } catch (NetworkException e) {
                    Application.getInstance().onError(R.string.CONNECTION_FAILED);
                }

                if (deleteHistory.isChecked()) {
                    MessageManager.getInstance().closeChat(account, user);
                    MessageManager.getInstance().clearHistory(account, user);
                    //AbstractChat chat = MessageManager.getInstance().getChat(account, user);
                    //if (chat != null) {
                    //    chat.setArchived(true, true);
                    //}
                }

                // remove roster contact
                RosterManager.getInstance().removeContact(account, user);

                dismiss();
                if (getActivity() instanceof ContactActivity) {
                    startActivity(ContactListActivity.createIntent(getActivity()));
                }
        }
    }
}
