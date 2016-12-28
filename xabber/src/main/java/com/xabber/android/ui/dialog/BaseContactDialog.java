package com.xabber.android.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.color.AccountPainter;
import com.xabber.android.ui.color.ColorManager;

public abstract class BaseContactDialog extends DialogFragment
        implements DialogInterface.OnClickListener, DialogInterface.OnShowListener {

    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.dialog.BaseContactDialog.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_CONTACT = "com.xabber.android.ui.dialog.BaseContactDialog.ARGUMENT_CONTACT";

    private AccountJid account;
    private UserJid contact;
    private AccountPainter accountPainter;
    private AlertDialog dialog;

    protected abstract int getDialogTitleTextResource();
    protected abstract String getMessage();
    protected abstract int getNegativeButtonTextResource();
    protected abstract int getPositiveButtonTextResource();
    protected abstract Integer getNeutralButtonTextResourceOrNull();
    protected abstract void onPositiveButtonClick();
    protected abstract void onNegativeButtonClick();
    protected abstract void onNeutralButtonClick();

    protected static void setArguments(AccountJid account, UserJid contact, DialogFragment fragment) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARGUMENT_ACCOUNT, account);
        arguments.putParcelable(ARGUMENT_CONTACT, contact);
        fragment.setArguments(arguments);
    }

    protected AccountJid getAccount() {
        return account;
    }

    protected UserJid getContact() {
        return contact;
    }

    protected void setUpContactTitleView(View view) {
        final AbstractContact bestContact = RosterManager.getInstance().getBestContact(account, contact);

        ((ImageView)view.findViewById(R.id.avatar)).setImageDrawable(bestContact.getAvatar());
        ((TextView)view.findViewById(R.id.name)).setText(bestContact.getName());
        ((TextView)view.findViewById(R.id.status_text)).setText(contact.toString());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        account = args.getParcelable(ARGUMENT_ACCOUNT);
        contact = args.getParcelable(ARGUMENT_CONTACT);

        accountPainter = ColorManager.getInstance().getAccountPainter();

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setCustomTitle(setUpDialogTitle())
                .setView(setUpDialogView())
                .setPositiveButton(getPositiveButtonTextResource(), this)
                .setNegativeButton(getNegativeButtonTextResource(), this);

        final Integer neutralButtonTextResource = getNeutralButtonTextResourceOrNull();
        if (neutralButtonTextResource != null) {
            builder.setNeutralButton(neutralButtonTextResource, this);
        }
        dialog = builder.create();
        dialog.setOnShowListener(this);

        return dialog;
    }

    @NonNull
    private View setUpDialogTitle() {
        View dialogTitleView = getActivity().getLayoutInflater().inflate(R.layout.dialog_title, null);
        final TextView dialogTitle = (TextView) dialogTitleView.findViewById(R.id.dialog_title_text_view);
        dialogTitle.setTextColor(accountPainter.getAccountTextColor(account));
        dialogTitle.setText(getDialogTitleTextResource());
        return dialogTitleView;
    }

    @NonNull
    private View setUpDialogView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.contact_title_dialog, null);
        setUpContactTitleView(view);
        ((TextView)view.findViewById(R.id.dialog_message)).setText(getMessage());
        return view;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        this.dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(accountPainter.getAccountTextColor(account));
        this.dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(accountPainter.getGreyMain());
        this.dialog.getButton(Dialog.BUTTON_NEUTRAL).setTextColor(accountPainter.getGreyMain());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                onPositiveButtonClick();
                break;

            case Dialog.BUTTON_NEUTRAL:
                onNeutralButtonClick();
                break;

            case Dialog.BUTTON_NEGATIVE:
                onNegativeButtonClick();
                break;
        }
    }
}

