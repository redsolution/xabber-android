package com.xabber.android.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;
import com.xabber.android.ui.dialog.AccountSyncDialogFragment;

/**
 * Created by valery.miller on 27.07.17.
 */

public class XabberAccountInfoFragment extends Fragment {

    private TextView tvAccountName;
    private TextView tvAccountUsername;
    private TextView tvLastSyncDate;
    private RelativeLayout rlLogout;
    private RelativeLayout rlSync;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xabber_account_info, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        tvAccountUsername = (TextView) view.findViewById(R.id.tvAccountUsername);
        tvLastSyncDate = (TextView) view.findViewById(R.id.tvLastSyncDate);

        rlLogout = (RelativeLayout) view.findViewById(R.id.rlLogout);
        rlLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });

        rlSync = (RelativeLayout) view.findViewById(R.id.rlSync);
        rlSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSyncDialog(false);
            }
        });

        if (getArguments().getBoolean("SHOW_SYNC", false))
            showSyncDialog(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) updateData(account);
        else ((XabberAccountInfoActivity)getActivity()).showLoginFragment();
        updateLastSyncTime();
    }

    public void updateData(@NonNull XabberAccount account) {
        String accountName = account.getFirstName() + " " + account.getLastName();
        if (accountName.trim().isEmpty())
            accountName = getString(R.string.title_xabber_account);

        tvAccountName.setText(accountName);
        if (account.getUsername() != null && !account.getUsername().isEmpty())
            tvAccountUsername.setText(getString(R.string.username, account.getUsername()));
    }

    public void updateLastSyncTime() {
        tvLastSyncDate.setText(getString(R.string.last_sync_date, SettingsManager.getLastSyncDate()));
    }

    public void showSyncDialog(boolean noCancel) {
        AccountSyncDialogFragment.newInstance(noCancel)
                .show(getFragmentManager(), AccountSyncDialogFragment.class.getSimpleName());
    }

    private void showLogoutDialog() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_logout_xabber_account, null);
        final CheckBox chbDeleteAccounts = (CheckBox) view.findViewById(R.id.chbDeleteAccounts);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.progress_title_logout)
                .setMessage(R.string.logout_summary)
                .setView(view)
                .setPositiveButton(R.string.button_logout, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((XabberAccountInfoActivity)getActivity()).onLogoutClick(chbDeleteAccounts.isChecked());
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        Dialog dialog = builder.create();
        dialog.show();
    }

}
