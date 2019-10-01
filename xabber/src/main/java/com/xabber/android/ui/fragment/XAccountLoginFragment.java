package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.QRCodeScannerActivity;
import com.xabber.android.ui.dialog.OrbotInstallerDialog;
import com.xabber.android.ui.helper.OnSocialBindListener;
import com.xabber.android.ui.helper.OrbotHelper;

import java.util.Collections;

public class XAccountLoginFragment extends Fragment implements View.OnClickListener {

    private CheckBox storePasswordView;
    private CheckBox chkSync;
    private CheckBox chkRequireTLS;
    private CheckBox chkUseTOR;
    private EditText edtUsername;
    private EditText edtPassword;
    private Button btnLogin;
    private Button btnOptions;
    private Button btnForgotPass;
    private ImageView imgQRcode;
    private ImageView imgClearText;
    private View optionsView;
    private View socialView;

    private OnSocialBindListener listener;
    private EmailClickListener emailListener;
    private ForgotPassClickListener forgotPassListener;

    public interface EmailClickListener {
        void onEmailClick();
    }

    public interface ForgotPassClickListener {
        void onForgotPassClick();
    }

    public static XAccountLoginFragment newInstance() {
        XAccountLoginFragment fragment = new XAccountLoginFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_xaccount_login, container, false);

        storePasswordView = (CheckBox) view.findViewById(R.id.store_password);
        chkSync = (CheckBox) view.findViewById(R.id.chkSync);
        if (XabberAccountManager.getInstance().getAccount() == null) {
            chkSync.setVisibility(View.GONE);
            chkSync.setChecked(false);
        }

        chkRequireTLS = view.findViewById(R.id.chkRequireTLS);
        chkUseTOR = view.findViewById(R.id.chkUseTOR);

        optionsView = view.findViewById(R.id.optionsView);
        socialView = view.findViewById(R.id.socialView);
        edtUsername = (EditText) view.findViewById(R.id.edtUsername);
        edtUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }


            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().equals("")) {
                    imgClearText.setVisibility(View.GONE);
                    imgQRcode.setVisibility(View.VISIBLE);
                } else {
                    imgClearText.setVisibility(View.VISIBLE);
                    imgQRcode.setVisibility(View.GONE);
                }
            }
        });
        edtPassword = (EditText) view.findViewById(R.id.edtPass);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(this);
        btnOptions = view.findViewById(R.id.btnOptions);
        btnOptions.setOnClickListener(this);
        btnForgotPass = view.findViewById(R.id.btnForgotPass);
        btnForgotPass.setOnClickListener(this);
        imgQRcode = view.findViewById(R.id.imgQRcode);
        imgQRcode.setOnClickListener(this);
        imgClearText = view.findViewById(R.id.imgCross);
        imgClearText.setOnClickListener(this);

        ((TextView) view.findViewById(R.id.account_help))
                .setMovementMethod(LinkMovementMethod.getInstance());

        ImageView ivFacebook = view.findViewById(R.id.ivFacebook);
        ImageView ivGoogle = view.findViewById(R.id.ivGoogle);
        ImageView ivTwitter = view.findViewById(R.id.ivTwitter);
        ImageView ivEmail = view.findViewById(R.id.ivEmail);

        ivFacebook.setOnClickListener(this);
        ivGoogle.setOnClickListener(this);
        ivTwitter.setOnClickListener(this);
        ivEmail.setOnClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSocialBindListener) listener = (OnSocialBindListener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement OnSocialBindListener");

        if (activity instanceof EmailClickListener) emailListener = (EmailClickListener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement EmailClickListener");

        if (activity instanceof ForgotPassClickListener)
            forgotPassListener = (ForgotPassClickListener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement ForgotPassClickListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        emailListener = null;
        forgotPassListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnLogin:
                addAccount();
                break;
            case R.id.ivFacebook:
                listener.onBindClick(AuthManager.PROVIDER_FACEBOOK);
                break;
            case R.id.ivGoogle:
                listener.onBindClick(AuthManager.PROVIDER_GOOGLE);
                break;
            case R.id.ivTwitter:
                listener.onBindClick(AuthManager.PROVIDER_TWITTER);
                break;
            case R.id.ivEmail:
                emailListener.onEmailClick();
                break;
            case R.id.btnOptions:
                showOptions();
                break;
            case R.id.btnForgotPass:
                forgotPassListener.onForgotPassClick();
                break;
            case R.id.imgQRcode:
                scanQRcode();
                break;
            case R.id.imgCross:
                clearUsername();
                break;
        }
    }

    private void clearUsername() {
        edtUsername.getText().clear();
    }

    private void scanQRcode(){
        IntentIntegrator integrator = IntentIntegrator.forFragment(this);
        integrator.setOrientationLocked(false)
                .setBeepEnabled(false)
                .setPrompt("")
                .setCameraId(0)
                .setCaptureActivity(QRCodeScannerActivity.class)
                .initiateScan(Collections.unmodifiableList(Collections.singletonList(IntentIntegrator.QR_CODE)));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getActivity(), "no-go", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Scanned = " + result.getContents(), Toast.LENGTH_LONG).show();
                if (result.getContents().length() > 5) {
                    String[] s = result.getContents().split(":");
                    if (s[0].equals("xmpp") && s.length>=2)
                        edtUsername.setText(s[1]);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addAccount() {
        if (chkUseTOR.isChecked() && !OrbotHelper.isOrbotInstalled()) {
            OrbotInstallerDialog.newInstance().show(getFragmentManager(), OrbotInstallerDialog.class.getName());
            return;
        }

        AccountJid account;
        try {
            account = AccountManager.getInstance().addAccount(
                    edtUsername.getText().toString().trim(),
                    edtPassword.getText().toString(),
                    "",
                    false,
                    storePasswordView.isChecked(),
                    chkSync.isChecked(),
                    chkUseTOR.isChecked(),
                    false, true,
                    chkRequireTLS.isChecked());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
            return;
        }

        startActivity(ContactListActivity.createIntent(getActivity()));
        getActivity().finish();
    }

    private void showOptions() {
        if (optionsView.getVisibility() == View.VISIBLE) {
            optionsView.setVisibility(View.GONE);
            socialView.setVisibility(View.VISIBLE);
            btnOptions.setText(R.string.button_advanced_options);
        } else {
            optionsView.setVisibility(View.VISIBLE);
            socialView.setVisibility(View.GONE);
            btnOptions.setText(R.string.button_hide_options);
        }
    }
}
