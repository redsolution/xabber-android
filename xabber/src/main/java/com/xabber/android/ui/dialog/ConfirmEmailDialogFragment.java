package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.xabber.android.R;

public class ConfirmEmailDialogFragment extends DialogFragment {

    private static final String KEY_EMAIL = "KEY_EMAIL";

    private String email;
    private Listener listener;

    private EditText edtCode;

    public interface Listener {
        void onResendCodeClick(String email);
        void onConfirmClick(String email, String code);
    }

    public static ConfirmEmailDialogFragment newInstance(String email) {
        ConfirmEmailDialogFragment fragment = new ConfirmEmailDialogFragment();
        fragment.email = email;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(setupView())
                .setTitle(R.string.title_email_confirm)
                .setNeutralButton(R.string.button_resend_link, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) listener.onResendCodeClick(email);
                    }
                })
                .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) listener.onConfirmClick(email, edtCode.getText().toString());
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
        else throw new RuntimeException(context.toString()
                + " must implement ConfirmEmailDialogFragment.Listener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_EMAIL, email);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getDialog().getWindow();
        if (window != null)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        if (savedInstanceState != null)
            email = savedInstanceState.getString(KEY_EMAIL);
    }

    public View setupView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_confirm_email, null);

        TextView tvDescription = view.findViewById(R.id.tvDescription);
        tvDescription.setText(getString(R.string.xmpp_confirm_title, email));
        edtCode = view.findViewById(R.id.edtCode);

        return view;
    }
}
