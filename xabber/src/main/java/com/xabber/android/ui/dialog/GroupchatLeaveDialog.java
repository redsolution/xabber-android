package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.roster.PresenceManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.OnChatUpdatedListener;
import com.xabber.android.ui.activity.ContactActivity;
import com.xabber.android.ui.activity.MainActivity;
import com.xabber.android.ui.color.ColorManager;

public class GroupchatLeaveDialog extends DialogFragment implements View.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.GroupchatLeaveDialog.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_GROUPCHAT = "com.xabber.android.ui.dialog.GroupchatLeaveDialog.ARGUMENT_GROUPCHAT";

    private ContactJid groupchatJid;
    private AccountJid account;

    public static GroupchatLeaveDialog newInstance(AccountJid account, ContactJid user) {
        GroupchatLeaveDialog fragment = new GroupchatLeaveDialog();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_GROUPCHAT, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_leave_groupchat, null);

        Bundle args = getArguments();

        if (args == null) dismiss();

        account = args.getParcelable(ARGUMENT_ACCOUNT);
        groupchatJid = args.getParcelable(ARGUMENT_GROUPCHAT);
        String contactName = RosterManager.getInstance().getBestContact(account, groupchatJid).getName();
        int colorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(account);

        ((TextView) view.findViewById(R.id.leave_groupchat_confirm))
                .setText(getString(R.string.groupchat_leave_confirm,
                        contactName));

        ((Button) view.findViewById(R.id.leave)).setTextColor(colorIndicator);

        view.findViewById(R.id.cancel_leave).setOnClickListener(this);
        view.findViewById(R.id.leave).setOnClickListener(this);

        return builder.setTitle(getString(R.string.groupchat_leave_full))
                .setView(view)
                .create();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_leave:
                dismiss();
                break;
            case R.id.leave:
                try {
                    // discard subscription
                    PresenceManager.INSTANCE.discardSubscription(account, groupchatJid);
                    PresenceManager.INSTANCE.unsubscribeFromPresence(account, groupchatJid);
                } catch (NetworkException e) {
                    Application.getInstance().onError(R.string.CONNECTION_FAILED);
                }

                // remove groupchat from roster
                RosterManager.getInstance().removeContact(account, groupchatJid);
                for (OnChatUpdatedListener listener :
                        Application.getInstance().getUIListeners(OnChatUpdatedListener.class)){
                    listener.onAction();
                }

                dismiss();
                if (getActivity() instanceof ContactActivity) {
                    startActivity(MainActivity.createIntent(getActivity()));
                }
        }
    }
}
