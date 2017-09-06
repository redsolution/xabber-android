package com.xabber.android.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountSettingsActivity;

import static com.xabber.android.data.account.AccountErrorEvent.Type.AUTHORIZATION;

/**
 * Created by valery.miller on 04.08.17.
 */

public class AccountEnterPassDialog extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARGUMENT_ERROR_EVENT = AccountErrorDialogFragment.class.getName() + "ARGUMENT_ERROR_EVENT";
    private AccountErrorEvent accountErrorEvent;
    private EditText edtPass;

    public static DialogFragment newInstance(AccountErrorEvent accountErrorEvent) {
        AccountEnterPassDialog fragment = new AccountEnterPassDialog();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARGUMENT_ERROR_EVENT, accountErrorEvent);

        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        accountErrorEvent = (AccountErrorEvent) args.getSerializable(ARGUMENT_ERROR_EVENT);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(AccountManager.getInstance().getVerboseName(accountErrorEvent.getAccount()))
                .setView(setUpDialogView())
                .setPositiveButton(R.string.login, this)
                .setNegativeButton(R.string.skip, this);

        if (accountErrorEvent!= null && accountErrorEvent.getType().equals(AUTHORIZATION)) {
            builder.setNeutralButton(R.string.settings, this);
        }

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    @NonNull
    private View setUpDialogView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_account_enter_pass, null);
        edtPass = (EditText) view.findViewById(R.id.edtPass);
        TextView mainTextView = (TextView) view.findViewById(R.id.account_error_main_text);
        final TextView detailTextView = (TextView) view.findViewById(R.id.account_error_detail_text);

        String message = getString(R.string.enter_password);
        if (accountErrorEvent!= null && accountErrorEvent.getType().equals(AUTHORIZATION)) {
            message = getString(R.string.auth_error);
        }

        mainTextView.setText(message);
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
                    if (!detailTextView.getText().toString().isEmpty()) {
                        detailTextView.setVisibility(View.VISIBLE);
                        expandIcon.setImageResource(R.drawable.ic_expand_less_grey600_24dp);
                    }
                }
            }
        });

        if (detailTextView.getText().toString().isEmpty())
            expandIcon.setVisibility(View.INVISIBLE);

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE) {
            if (edtPass != null) {
                String password = edtPass.getText().toString();
                AccountManager.getInstance().updateAccountPassword(accountErrorEvent.getAccount(), password);
            }
        }
        if (which == Dialog.BUTTON_NEGATIVE) {
            dialog.dismiss();
        }
        if (which == Dialog.BUTTON_NEUTRAL) {
            Activity activity = getActivity();

            if (activity instanceof AccountActivity) {
                activity.finish();
            }

            if (activity instanceof AccountSettingsActivity) {
                AccountManager.getInstance().removeAccountError(accountErrorEvent.getAccount());
            } else {
                startActivity(AccountActivity.createConnectionSettingsIntent(activity, accountErrorEvent.getAccount()));
            }
        }
    }
}
