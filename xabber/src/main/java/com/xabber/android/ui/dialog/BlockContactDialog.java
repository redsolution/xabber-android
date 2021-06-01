package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.blocking.BlockingManager;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.message.chat.ChatAction;
import com.xabber.android.data.message.chat.ChatManager;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.color.ColorManager;

import org.greenrobot.eventbus.EventBus;

public class BlockContactDialog extends DialogFragment implements BlockingManager.BlockContactListener, View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.ContactBlocker.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.xabber.android.ui.dialog.ContactBlocker.ARGUMENT_USER";

    private AccountJid account;
    private ContactJid user;
    private boolean andDelete = false;

    public static BlockContactDialog newInstance(AccountJid account, ContactJid user) {
        BlockContactDialog fragment = new BlockContactDialog();

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
        Context context = builder.getContext();
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.dialog_block_contact, null);

        Bundle args = getArguments();

        if (args == null) dismiss();

        account = args.getParcelable(ARGUMENT_ACCOUNT);
        user = args.getParcelable(ARGUMENT_USER);
        String contactName = RosterManager.getInstance().getBestContact(account, user).getName();
        String accountName = AccountManager.getInstance().getVerboseName(account);
        int colorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account);
        int buttonColor = SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark ?
                getResources().getColor(R.color.red_700) : getResources().getColor(R.color.red_900);


        ((TextView) view.findViewById(R.id.block_contact_confirm))
                .setText(Html.fromHtml(getString(R.string.block_contact_confirm_short, contactName)));

        ((TextView) view.findViewById(R.id.block_contact_warning))
                .setText(Html.fromHtml(getString(R.string.block_contact_warning, user.getBareJid().toString())));

        ((Button) view.findViewById(R.id.block_and_delete)).setTextColor(colorIndicator);
        ((Button) view.findViewById(R.id.block)).setTextColor(colorIndicator);

        view.findViewById(R.id.cancel_block).setOnClickListener(this);
        view.findViewById(R.id.block_and_delete).setOnClickListener(this);
        view.findViewById(R.id.block).setOnClickListener(this);

        return builder.setTitle(R.string.contact_block)
                .setView(view)
                .create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.block_and_delete:
                andDelete = true;
            case R.id.block:
                discardSubscription();
                BlockingManager.getInstance().blockContact(account, user, this);
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
        EventBus.getDefault().post(new ChatManager.ChatUpdatedEvent());
    }

    @Override
    public void onErrorBlock() {
        Toast.makeText(Application.getInstance(), R.string.error_blocking_contact, Toast.LENGTH_SHORT).show();
    }

    private void discardSubscription() {
        try {
            // discard subscription
            PresenceManager.getInstance().discardSubscription(account, user);
            PresenceManager.getInstance().unsubscribeFromPresence(account, user);
        } catch (NetworkException e) {
            Application.getInstance().onError(R.string.CONNECTION_FAILED);
        }
    }

    private void deleteContact() {
        ChatManager.getInstance().closeChat(account, user);

        // delete chat
        AbstractChat chat = ChatManager.getInstance().getChat(account, user);
        if (chat != null) {
            chat.setArchived(true);
        }

        // remove roster contact
        RosterManager.getInstance().removeContact(account, user);
        if (chat != null) {
            chat.newSilentAction(null, Application.getInstance().getString(R.string.action_contact_deleted), ChatAction.contact_deleted, false);
        }

        if (getActivity() instanceof ContactActivity) {
            startActivity(MainActivity.createIntent(getActivity()));
        }
    }

}
