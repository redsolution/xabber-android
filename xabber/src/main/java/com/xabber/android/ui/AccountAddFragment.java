package com.xabber.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.NetworkException;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.AccountType;
import com.xabber.android.ui.adapter.AccountTypeAdapter;
import com.xabber.android.ui.dialog.OrbotInstallerDialogBuilder;
import com.xabber.android.ui.helper.OrbotHelper;
import com.xabber.android.ui.preferences.AccountEditor;

public class AccountAddFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String SAVED_ACCOUNT_TYPE = "com.xabber.android.ui.AccountAdd.ACCOUNT_TYPE";
    private CheckBox storePasswordView;
    private CheckBox useOrbotView;
    private CheckBox createAccountCheckBox;
    private Spinner accountTypeView;
    private LinearLayout passwordConfirmView;
    private EditText userView;
    private EditText passwordView;
    private EditText passwordConfirmEditText;
    private View authPanel;
    private TextView accountHelpView;

    public static AccountAddFragment newInstance() {
        return new AccountAddFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.account_add_fragment, container, false);

        accountTypeView = (Spinner) view.findViewById(R.id.account_type);
        accountTypeView.setAdapter(new AccountTypeAdapter(getActivity()));
        accountTypeView.setOnItemSelectedListener(this);

        String accountType;
        if (savedInstanceState == null) {
            accountType = null;
        } else {
            accountType = savedInstanceState.getString(SAVED_ACCOUNT_TYPE);
        }

        accountTypeView.setSelection(0);
        for (int position = 0; position < accountTypeView.getCount(); position++) {
            if (((AccountType) accountTypeView.getItemAtPosition(position)).getName().equals(accountType)){
                accountTypeView.setSelection(position);
                break;
            }
        }

        storePasswordView = (CheckBox) view.findViewById(R.id.store_password);
        useOrbotView = (CheckBox) view.findViewById(R.id.use_orbot);
        createAccountCheckBox = (CheckBox) view.findViewById(R.id.register_account);
        createAccountCheckBox.setOnClickListener(this);

        authPanel = view.findViewById(R.id.auth_panel);

        userView = (EditText) view.findViewById(R.id.account_user_name);
        accountHelpView = (TextView) view.findViewById(R.id.account_help);
        passwordView = (EditText) view.findViewById(R.id.account_password);
        passwordConfirmEditText = (EditText) view.findViewById(R.id.confirm_password);

        passwordConfirmView = (LinearLayout) view.findViewById(R.id.confirm_password_layout);

        return view;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_ACCOUNT_TYPE, ((AccountType) accountTypeView.getSelectedItem()).getName());
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

        AccountType accountType = (AccountType) accountTypeView.getSelectedItem();

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
        startActivity(AccountEditor.createIntent(getActivity(), account));
        getActivity().finish();
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        AccountType accountType = (AccountType) accountTypeView.getSelectedItem();
        authPanel.setVisibility(View.VISIBLE);
        accountHelpView.setText(accountType.getHelp());
        userView.setHint(accountType.getHint());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        accountTypeView.setSelection(0);
    }
}
