package com.xabber.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.AccountType;
import com.xabber.android.ui.dialog.OrbotInstallerDialogBuilder;
import com.xabber.android.ui.helper.OrbotHelper;
import com.xabber.androiddev.R;

public class AccountAddFragment extends Fragment implements View.OnClickListener {

    private CheckBox storePasswordView;
    private CheckBox useOrbotView;
    private CheckBox createAccountCheckBox;
    private AccountType accountType;
    private LinearLayout passwordConfirmView;
    private EditText userView;
    private EditText passwordView;
    private EditText passwordConfirmEditText;


    public static AccountAddFragment newInstance() {
        return new AccountAddFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.account_add_fragment, container, false);

        accountType = AccountManager.getInstance().getAccountTypes().get(0);

        storePasswordView = (CheckBox) view.findViewById(R.id.store_password);
        useOrbotView = (CheckBox) view.findViewById(R.id.use_orbot);
        createAccountCheckBox = (CheckBox) view.findViewById(R.id.register_account);
        createAccountCheckBox.setOnClickListener(this);

        userView = (EditText) view.findViewById(R.id.account_user_name);
        passwordView = (EditText) view.findViewById(R.id.account_password);
        passwordConfirmEditText = (EditText) view.findViewById(R.id.confirm_password);

        passwordConfirmView = (LinearLayout) view.findViewById(R.id.confirm_password_layout);

        view.findViewById(R.id.auth_panel).setVisibility(View.VISIBLE);
        userView.setHint(accountType.getHint());
        ((TextView) view.findViewById(R.id.account_help)).setText(accountType.getHelp());

        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_account:
                if(createAccountCheckBox.isChecked()) {
                    passwordConfirmView.setVisibility(View.VISIBLE);
                } else {
                    passwordConfirmView.setVisibility(View.GONE);
                }
            default:
                break;
        }
    }

    public void addAccount() {
        if (useOrbotView.isChecked() && !OrbotHelper.isOrbotInstalled()) {
            OrbotInstallerDialogBuilder.show(getActivity());
            return;
        }

        if (createAccountCheckBox.isChecked() &&
                !passwordView.getText().toString().contentEquals(passwordConfirmEditText.getText().toString())) {
            Toast.makeText(getActivity(), getString(R.string.CONFIRM_PASSWORD), Toast.LENGTH_LONG).show();
            return;
        }
        String account;
        try {
            account = AccountManager.getInstance().addAccount(
                    userView.getText().toString(),
                    passwordView.getText().toString(), accountType,
                    false,
                    storePasswordView.isChecked(),
                    useOrbotView.isChecked(),
                    createAccountCheckBox.isChecked());
        } catch (NetworkException e) {
            Application.getInstance().onError(e);
            return;
        }

        getActivity().setResult(Activity.RESULT_OK, AccountAdd.createAuthenticatorResult(account));
        getActivity().finish();
    }


}
