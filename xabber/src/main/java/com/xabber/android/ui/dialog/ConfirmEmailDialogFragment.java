package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.xabber.android.R;

public class ConfirmEmailDialogFragment extends DialogFragment {

    private String email;

    public static ConfirmEmailDialogFragment newInstance(String email) {
        ConfirmEmailDialogFragment fragment = new ConfirmEmailDialogFragment();
        fragment.email = email;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(setupView())
                .setTitle("Confirm email")
                .setNeutralButton("Resend code", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //listener.onResendCodeClick();
                    }
                })
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //listener.onConfirmEmailClick();
                    }
                });

        return builder.create();
    }

    public View setupView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_confirm_email, null);

        TextView tvDescription = view.findViewById(R.id.tvDescription);
        tvDescription.setText(getString(R.string.xmpp_confirm_title, email));

        return view;
    }
}
