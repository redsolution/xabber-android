package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.xabber.android.R;

public class XAccountEmailLoginFragment extends Fragment {

    private EditText edtEmail;
    private EditText edtPass;
    private Button btnLogin;
    private Button btnForgotPass;

    private Listener listener;
    private XAccountLoginFragment.ForgotPassClickListener forgotPassListener;

    public interface Listener {
        void onLoginClick(String email, String pass);
    }

    public static XAccountEmailLoginFragment newInstance() {
        return new XAccountEmailLoginFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_email_login, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtEmail = view.findViewById(R.id.edtEmail);
        edtPass = view.findViewById(R.id.edtPass);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();
                String pass = edtPass.getText().toString().trim();
                if (verifyFields(email, pass)) listener.onLoginClick(email, pass);
            }
        });
        btnForgotPass = view.findViewById(R.id.btnForgotPass);
        btnForgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forgotPassListener.onForgotPassClick();
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) listener = (Listener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement XAccountEmailLoginFragment.Listener");

        if (activity instanceof XAccountLoginFragment.ForgotPassClickListener)
            forgotPassListener = (XAccountLoginFragment.ForgotPassClickListener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement ForgotPassClickListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        forgotPassListener = null;
    }

    private boolean verifyFields(String email, String pass) {
        if (email.isEmpty()) {
            edtEmail.setError(getString(R.string.empty_field));
            return false;
        }

        if (pass.isEmpty()) {
            edtPass.setError(getString(R.string.empty_field));
            return false;
        }

        return true;
    }
}
