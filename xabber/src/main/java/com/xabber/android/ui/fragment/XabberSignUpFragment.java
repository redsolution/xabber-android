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
import android.widget.RelativeLayout;

import com.xabber.android.R;
import com.xabber.android.ui.activity.XabberLoginActivity;

/**
 * Created by valery.miller on 25.07.17.
 */

public class XabberSignUpFragment extends Fragment implements View.OnClickListener {

    private AppCompatCheckBox checkBox;
    private Button btnSignup;
    private RelativeLayout rlLogin;

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

        checkBox = (AppCompatCheckBox) view.findViewById(R.id.chbAgrees);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnSignup.setEnabled(isChecked);
            }
        });

        rlLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rlLogin:
                ((XabberLoginActivity)getActivity()).showLoginFragment();
                break;
        }
    }
}
