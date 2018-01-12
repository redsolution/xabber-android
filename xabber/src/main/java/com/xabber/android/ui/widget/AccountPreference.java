package com.xabber.android.ui.widget;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;

/**
 * Created by valery.miller on 27.07.17.
 */

public class AccountPreference extends Preference {

    public AccountPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        return li.inflate( R.layout.preference_account, parent, false);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        LinearLayout llAccountInfo = (LinearLayout) view.findViewById(R.id.accountInfo);
        LinearLayout llNoAccount = (LinearLayout) view.findViewById(R.id.noAccount);
        TextView tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        TextView tvAccountUsername = (TextView) view.findViewById(R.id.tvAccountUsername);
        XabberAccount account = XabberAccountManager.getInstance().getAccount();

        if (account != null) {
            llAccountInfo.setVisibility(View.VISIBLE);
            llNoAccount.setVisibility(View.GONE);

            String accountName = account.getFirstName() + " " + account.getLastName();
            if (accountName.trim().isEmpty())
                accountName = getContext().getString(R.string.title_xabber_account);

            if (XabberAccount.STATUS_NOT_CONFIRMED.equals(account.getAccountStatus())) {
                tvAccountName.setText(accountName);
                tvAccountUsername.setText(R.string.title_email_confirm);
            }
            if (XabberAccount.STATUS_CONFIRMED.equals(account.getAccountStatus())) {
                tvAccountName.setText(accountName);
                tvAccountUsername.setText(R.string.title_complete_register);
            }
            if (XabberAccount.STATUS_REGISTERED.equals(account.getAccountStatus())) {
                tvAccountName.setText(accountName);
                tvAccountUsername.setText(account.getUsername());
            }
        } else {
            llAccountInfo.setVisibility(View.GONE);
            llNoAccount.setVisibility(View.VISIBLE);
        }
    }
}
