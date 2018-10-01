package com.xabber.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
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

    public static AddEmailDialogFragment newInstance() {
        AddEmailDialogFragment fragment = new AddEmailDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(setupView())
                .setTitle(R.string.title_add_email)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton(R.string.action_connect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (listener != null) listener.onAddEmailClick(edtEmail.getText().toString());
                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
        else throw new RuntimeException(context.toString()
                + " must implement AddEmailDialogFragment.Listener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public View setupView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_email, null);

        edtEmail = view.findViewById(R.id.edtEmail);

        return view;
    }
}
