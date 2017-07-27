package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;

/**
 * Created by valery.miller on 27.07.17.
 */

public class XabberAccountCompleteRegsiterFrament extends Fragment {

    private TextView tvAccountEmail;
    private RelativeLayout rlLogout;

    private EditText edtUsername;
    private EditText edtPass;
    private EditText edtPass2;
    private EditText edtFirstName;
    private EditText edtLastName;
    private EditText edtHost;
    private Button btnComplete;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xabber_account_complete_register, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAccountEmail = (TextView) view.findViewById(R.id.tvAccountEmail);

        rlLogout = (RelativeLayout) view.findViewById(R.id.rlLogout);
        rlLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((XabberAccountInfoActivity)getActivity()).onLogoutClick();
            }
        });

        edtUsername = (EditText) view.findViewById(R.id.edtUsername);
        edtPass = (EditText) view.findViewById(R.id.edtPass);
        edtPass2 = (EditText) view.findViewById(R.id.edtPass2);
        edtFirstName = (EditText) view.findViewById(R.id.edtFirstName);
        edtLastName = (EditText) view.findViewById(R.id.edtLastName);
        edtHost = (EditText) view.findViewById(R.id.edtHost);
        btnComplete = (Button) view.findViewById(R.id.btnComplete);
        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyFields();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) updateData(account);
        else ((XabberAccountInfoActivity)getActivity()).showLoginFragment();
    }

    public void updateData(@NonNull XabberAccount account) {
        tvAccountEmail.setText(account.getEmails().get(0).getEmail());
    }

    private void verifyFields() {
        String username = edtUsername.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();
        String pass2 = edtPass2.getText().toString().trim();
        String firstName = edtFirstName.getText().toString().trim();
        String lastName = edtLastName.getText().toString().trim();
        String host = edtHost.getText().toString().trim();

        if (username.isEmpty()) {
            edtUsername.setError(getString(R.string.empty_field));
            return;
        }

        // Username can contain only latin letters, numbers and dots
        if (!username.matches("[a-z0-9.]+")) {
            edtUsername.setError(getString(R.string.username_rules));
            return;
        }

        // Username must start with letter
        if (!username.substring(0, 1).matches("[a-z]")) {
            edtUsername.setError(getString(R.string.username_rules_first_symbol));
            return;
        }

        // Username cannot have two dots in a row
        if (username.contains("..")) {
            edtUsername.setError(getString(R.string.username_rules_dots));
            return;
        }

        if (pass.isEmpty()) {
            edtPass.setError(getString(R.string.empty_field));
            return;
        }

        if (pass2.isEmpty()) {
            edtPass2.setError(getString(R.string.empty_field));
            return;
        }

        if (!pass.equals(pass2)) {
            edtPass2.setError(getString(R.string.passwords_not_match));
            return;
        }

        ((XabberAccountInfoActivity)getActivity()).onCompleteClick(username, pass, pass2, firstName, lastName, host);
    }

}
