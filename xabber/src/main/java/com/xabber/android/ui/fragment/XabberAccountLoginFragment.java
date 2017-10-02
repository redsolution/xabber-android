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
import android.widget.ImageView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.connection.NetworkManager;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;

/**
 * Created by valery.miller on 27.07.17.
 */

public class XabberAccountLoginFragment extends Fragment implements View.OnClickListener {

    private ImageView ivGoogle;
    private ImageView ivFacebook;
    private ImageView ivTwitter;
    private ImageView ivGithub;

    private EditText edtEmail;
    private AppCompatCheckBox checkBox;
    private Button btnContinue;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xabber_account_login, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnContinue = (Button) view.findViewById(R.id.btnContinue);
        edtEmail = (EditText) view.findViewById(R.id.edtEmail);

        checkBox = (AppCompatCheckBox) view.findViewById(R.id.chbAgrees);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnContinue.setEnabled(isChecked);
            }
        });

        ivFacebook = (ImageView) view.findViewById(R.id.ivFacebook);
        ivGoogle = (ImageView) view.findViewById(R.id.ivGoogle);
        ivTwitter = (ImageView) view.findViewById(R.id.ivTwitter);
        ivGithub = (ImageView) view.findViewById(R.id.ivGithub);

        btnContinue.setOnClickListener(this);
        ivFacebook.setOnClickListener(this);
        ivGoogle.setOnClickListener(this);
        ivTwitter.setOnClickListener(this);
        ivGithub.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnContinue:
                onContinueClick();
                break;
            case R.id.ivFacebook:
                ((XabberAccountInfoActivity)getActivity()).loginFacebook();
                break;
            case R.id.ivGoogle:
                ((XabberAccountInfoActivity)getActivity()).loginGoogle();
                break;
            case R.id.ivGithub:
                ((XabberAccountInfoActivity)getActivity()).loginGithub();
                break;
            case R.id.ivTwitter:
                ((XabberAccountInfoActivity)getActivity()).loginTwitter();
                break;
        }
    }

    private void onContinueClick() {
        String email = edtEmail.getText().toString().trim();

        if (email.isEmpty()) {
            edtEmail.setError(getString(R.string.empty_field));
            return;
        }

        if (NetworkManager.isNetworkAvailable()) {
            ((XabberAccountInfoActivity)getActivity()).signup(email);
        } else
            Toast.makeText(getActivity(), R.string.toast_no_internet, Toast.LENGTH_LONG).show();
    }
}
