package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.ui.activity.XabberLoginActivity;

/**
 * Created by valery.miller on 25.07.17.
 */

public class XabberSignUpFragment extends Fragment implements View.OnClickListener {

    private AppCompatCheckBox checkBox;
    private Button btnSignup;
    private RelativeLayout rlLogin;
    private EditText edtEmail;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.include_signup_fields, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rlLogin = (RelativeLayout) view.findViewById(R.id.rlLogin);
        btnSignup = (Button) view.findViewById(R.id.btnSignup);
        edtEmail = (EditText) view.findViewById(R.id.edtEmail);

        checkBox = (AppCompatCheckBox) view.findViewById(R.id.chbAgrees);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnSignup.setEnabled(isChecked);
            }
        });

        rlLogin.setOnClickListener(this);
        btnSignup.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rlLogin:
                ((XabberLoginActivity)getActivity()).showLoginFragment();
                break;
            case R.id.btnSignup:
                onSignUpClick();
                break;
        }
    }

    private void onSignUpClick() {
        String email = edtEmail.getText().toString().trim();

        if (email.isEmpty()) {
            edtEmail.setError(getString(R.string.empty_field));
            return;
        }

        if (NetworkManager.isNetworkAvailable()) {
            ((XabberLoginActivity)getActivity()).signup(email);
        } else
            Toast.makeText(getActivity(), R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }
}
