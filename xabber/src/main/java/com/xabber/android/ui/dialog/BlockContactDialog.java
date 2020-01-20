package com.xabber.android.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.ContactListActivity;

public class BlockContactDialog extends DialogFragment implements BlockingManager.BlockContactListener, View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ContactBlocker.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ContactBlocker.ARGUMENT_USER";

    private AccountJid account;
    private UserJid user;
    private boolean andDelete = false;

    public static BlockContactDialog newInstance(AccountJid account, UserJid user) {
        BlockContactDialog fragment = new BlockContactDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_block_contact, container, false);
        Bundle args = getArguments();

        if (args == null) dismiss();

        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);
        String contactName = RosterManager.getInstance().getBestContact(account, user).getName();
        String accountName = AccountManager.getInstance().getVerboseName(account);

        ((TextView) view.findViewById(R.id.block_contact_confirm))
                .setText(String.format(getResources().getString(R.string.block_contact_confirm),
                        contactName, accountName));

        ((TextView) view.findViewById(R.id.block_contact_warning))
                .setText(String.format(getResources().getString(R.string.block_contact_warning),
                        user.getBareJid().toString()));

        ((Button) view.findViewById(R.id.block_and_delete)).setTextColor(getResources().getColor(R.color.red_900));
        ((Button) view.findViewById(R.id.block)).setTextColor(getResources().getColor(R.color.red_900));

        view.findViewById(R.id.cancel_block).setOnClickListener(this);
        view.findViewById(R.id.block_and_delete).setOnClickListener(this);
        view.findViewById(R.id.block).setOnClickListener(this);

        return view;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.block_and_delete:
                andDelete = true;
            case R.id.block:
                BlockingManager.getInstance().blockContact(account, user, this);
                break;
            case R.id.cancel_block:
                dismiss();
                break;
        }
    }

    @Override
    public void onSuccessBlock() {
        Toast.makeText(Application.getInstance(), R.string.contact_blocked_successfully, Toast.LENGTH_SHORT).show();
        if (andDelete){
            deleteContact();
        }
        dismiss();
    }

    @Override
    public void onErrorBlock() {
        Toast.makeText(Application.getInstance(), R.string.error_blocking_contact, Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void deleteContact() {
        MessageManager.getInstance().closeChat(account, user);

        try {
            // discard subscription
            PresenceManager.getInstance().discardSubscription(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(R.string.CONNECTION_FAILED);
        }

        // delete chat
        AbstractChat chat = MessageManager.getInstance().getChat(account, user);
        if (chat != null) {
            chat.setArchived(true, true);
        }

        // remove roster contact
        RosterManager.getInstance().removeContact(account, user);

        if (getActivity() instanceof ContactActivity) {
            startActivity(ContactListActivity.createIntent(getActivity()));
        }
    }

}
