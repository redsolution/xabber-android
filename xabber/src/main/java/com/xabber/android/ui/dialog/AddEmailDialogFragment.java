package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.xabber.android.R;

public class AddEmailDialogFragment extends DialogFragment {

    private Listener listener;
    private EditText edtEmail;

    public interface Listener {
        void onAddEmailClick(String email);
    }

    public static AddEmailDialogFragment newInstance(Listener listener) {
        AddEmailDialogFragment fragment = new AddEmailDialogFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(setupView())
                .setTitle("Add email")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onAddEmailClick(edtEmail.getText().toString());
                    }
                });

        return builder.create();
    }

    public View setupView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_email, null);

        edtEmail = view.findViewById(R.id.edtEmail);

        return view;
    }
}
