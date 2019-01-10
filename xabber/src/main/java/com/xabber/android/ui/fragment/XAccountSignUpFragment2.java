package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.xabber.android.R;

public class XAccountSignUpFragment2 extends Fragment implements View.OnClickListener {

    private Listener listener;

    private EditText edtPass;
    private Button btnNext;

    public interface Listener {
        void on2StepCompleted(String pass);
    }

    public static XAccountSignUpFragment2 newInstance(Listener listener) {
        XAccountSignUpFragment2 fragment = new XAccountSignUpFragment2();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_signup_2, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtPass = view.findViewById(R.id.edtPass);
        edtPass.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnNext.setEnabled(s.length() >= 4);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnNext = view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(this);
        btnNext.setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnNext:
                String pass = edtPass.getText().toString().trim();
                if (verifyFields(pass) && listener != null) listener.on2StepCompleted(pass);
                break;
        }
    }

    private boolean verifyFields(String pass) {

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
}
