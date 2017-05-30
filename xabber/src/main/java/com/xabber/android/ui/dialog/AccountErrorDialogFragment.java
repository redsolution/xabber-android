package com.xabber.android.ui.dialog;

import android.app.Activity;
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
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountSettingsActivity;


public class AccountErrorDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String ARGUMENT_ACCOUNT = AccountErrorDialogFragment.class.getName() + "ARGUMENT_ACCOUNT";
    private static final String ARGUMENT_ERROR_EVENT = AccountErrorDialogFragment.class.getName() + "ARGUMENT_ERROR_EVENT";
    private AccountErrorEvent accountErrorEvent;

    public static DialogFragment newInstance(AccountErrorEvent accountErrorEvent) {
        AccountErrorDialogFragment fragment = new AccountErrorDialogFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARGUMENT_ERROR_EVENT, accountErrorEvent);

        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        accountErrorEvent = (AccountErrorEvent) args.getSerializable(ARGUMENT_ERROR_EVENT);

        return new AlertDialog.Builder(getActivity())
                .setTitle(AccountManager.getInstance().getVerboseName(accountErrorEvent.getAccount()))
                .setView(setUpDialogView())
                .setPositiveButton(R.string.account_error_settings, this)
                .create();
    }

    @NonNull
    private View setUpDialogView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_account_error, null);

        TextView mainTextView = (TextView) view.findViewById(R.id.account_error_main_text);

        switch (accountErrorEvent.getType()) {

            case AUTHORIZATION:
                mainTextView.setText(R.string.AUTHENTICATION_FAILED);
                break;
            case CONNECTION:
                mainTextView.setText(R.string.CONNECTION_FAILED);
                break;
            case PASS_REQUIRED:
                mainTextView.setText(R.string.PASSWORD_REQUIRED);
                break;
        }

        final TextView detailTextView = (TextView) view.findViewById(R.id.account_error_detail_text);

        detailTextView.setText(accountErrorEvent.getMessage());
        detailTextView.setVisibility(View.GONE);

        final ImageView expandIcon = (ImageView) view.findViewById(R.id.account_error_expand_icon);

        View mainTextPanel = view.findViewById(R.id.account_error_main_text_panel);

        mainTextPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (detailTextView.getVisibility() == View.VISIBLE) {
                    detailTextView.setVisibility(View.GONE);
                    expandIcon.setImageResource(R.drawable.ic_expand_more_grey600_24dp);
                } else {
                    detailTextView.setVisibility(View.VISIBLE);
                    expandIcon.setImageResource(R.drawable.ic_expand_less_grey600_24dp);
                }
            }
        });

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            Activity activity = getActivity();

            if (activity instanceof AccountActivity) {
                activity.finish();
            }

            if (activity instanceof  AccountSettingsActivity) {
                AccountManager.getInstance().removeAccountError(accountErrorEvent.getAccount());
            } else {
                startActivity(AccountActivity.createConnectionSettingsIntent(activity, accountErrorEvent.getAccount()));
            }
        }
    }

}
