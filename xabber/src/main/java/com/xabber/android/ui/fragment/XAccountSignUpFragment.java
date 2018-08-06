package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.xabber.android.R;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;

import java.util.List;

public class XAccountSignUpFragment extends Fragment implements View.OnClickListener {

    private EditText edtUsername;
    private EditText edtPass;
    private Spinner spinnerDomain;
    private Button btnSignUp;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_signup, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtUsername = view.findViewById(R.id.edtUsername);
        edtPass = view.findViewById(R.id.edtPass);
        spinnerDomain = view.findViewById(R.id.spinnerDomain);
        btnSignUp = view.findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((XabberAccountInfoActivity)getActivity()).getHosts();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSignUp:
                onSignUpClick();
                break;
        }
    }

    private void onSignUpClick() {
        if (verifyFields()) {
            // TODO: 03.08.18 call API
        }
    }

    private boolean verifyFields() {
        String username = edtUsername.getText().toString().trim();
        String pass = edtPass.getText().toString().trim();

        if (username.isEmpty()) {
            edtUsername.setError(getString(R.string.empty_field));
            return false;
        }

        // Username can contain only latin letters, numbers and dots
        if (!username.matches("[a-z0-9.]+")) {
            edtUsername.setError(getString(R.string.username_rules));
            return false;
        }

        // Username must start with letter
        if (!username.substring(0, 1).matches("[a-z]")) {
            edtUsername.setError(getString(R.string.username_rules_first_symbol));
            return false;
        }

        // Username cannot have two dots in a row
        if (username.contains("..")) {
            edtUsername.setError(getString(R.string.username_rules_dots));
            return false;
        }

        if (pass.isEmpty()) {
            edtPass.setError(getString(R.string.empty_field));
            return false;
        }

        if (pass.length() < 4) {
            edtPass.setError(getString(R.string.pass_too_short));
            return false;
        }

        return true;
    }

    public void setupSpinner(List<String> domains) {
        if (spinnerDomain == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, domains);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDomain.setAdapter(adapter);
    }
}
