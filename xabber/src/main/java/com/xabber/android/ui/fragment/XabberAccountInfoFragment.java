package com.xabber.android.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.EmailDTO;
import com.xabber.android.data.xaccount.SocialBindingDTO;
import com.xabber.android.data.xaccount.XMPPAccountSettings;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.EmailAdapter;
import com.xabber.android.ui.dialog.AccountSyncDialogFragment;
import com.xabber.android.ui.dialog.AddEmailDialogFragment;
import com.xabber.android.ui.dialog.ConfirmEmailDialogFragment;
import com.xabber.android.utils.RetrofitErrorConverter;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 27.07.17.
 */

public class XabberAccountInfoFragment extends Fragment implements AddEmailDialogFragment.Listener,
        EmailAdapter.Listener, ConfirmEmailDialogFragment.Listener, AccountSyncDialogFragment.Listener {

    private static final String LOG_TAG = XabberAccountInfoFragment.class.getSimpleName();

    private TextView tvAccountName;
    private TextView tvAccountUsername;
    private TextView tvLanguage;
    private TextView tvPhone;
    private TextView tvLastSyncDate;
    private RelativeLayout rlLogout;
    private RelativeLayout rlSync;

    private TextView tvStatusGoogle;
    private TextView tvActionGoogle;
    private LinearLayout itemGoogle;
    private TextView tvStatusFacebook;
    private TextView tvActionFacebook;
    private LinearLayout itemFacebook;
    private TextView tvStatusTwitter;
    private TextView tvActionTwitter;
    private LinearLayout itemTwitter;

    private RecyclerView rvEmails;
    private EmailAdapter emailAdapter;
    private Button btnAddEmail;

    private boolean dialogShowed;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private Listener listener;

    public interface Listener {
        void onSocialBindClick(String provider);
        void onSocialUnbindClick(String provider);
        void onAddEmailClick(String email);
        void onConfirmEmailClick(String email, String code);
        void onLogoutClick(boolean deleteAccounts);
        void onSyncClick(boolean needGoToMainActivity);
        void needXMPPAuthFragment();
    }

    public static XabberAccountInfoFragment newInstance(Listener listener) {
        XabberAccountInfoFragment fragment = new XabberAccountInfoFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xabber_account_info, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPhone = (TextView) view.findViewById(R.id.tvPhoneNumber);
        tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        tvAccountUsername = (TextView) view.findViewById(R.id.tvAccountUsername);
        tvLanguage = (TextView) view.findViewById(R.id.tvLanguage);
        tvLastSyncDate = (TextView) view.findViewById(R.id.tvLastSyncDate);

        rlLogout = (RelativeLayout) view.findViewById(R.id.rlLogout);
        rlLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dialogShowed) {
                    dialogShowed = true;
                    showLogoutDialog();
                }
            }
        });

        rlSync = (RelativeLayout) view.findViewById(R.id.rlSync);
        rlSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dialogShowed) {
                    dialogShowed = true;
                    showSyncDialog(false);
                }
            }
        });

        tvStatusGoogle = view.findViewById(R.id.tvStatusGoogle);
        tvActionGoogle = view.findViewById(R.id.tvActionGoogle);
        itemGoogle = view.findViewById(R.id.itemGoogle);
        itemGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvActionGoogle.getText().equals(getString(R.string.action_connect)))
                    listener.onSocialBindClick(AuthManager.PROVIDER_GOOGLE);
                else listener.onSocialUnbindClick(AuthManager.PROVIDER_GOOGLE);
            }
        });

        tvStatusFacebook = view.findViewById(R.id.tvStatusFacebook);
        tvActionFacebook = view.findViewById(R.id.tvActionFacebook);
        itemFacebook = view.findViewById(R.id.itemFacebook);
        itemFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvActionGoogle.getText().equals(getString(R.string.action_connect)))
                    listener.onSocialBindClick(AuthManager.PROVIDER_FACEBOOK);
                else listener.onSocialUnbindClick(AuthManager.PROVIDER_FACEBOOK);
            }
        });

        tvStatusTwitter = view.findViewById(R.id.tvStatusTwitter);
        tvActionTwitter = view.findViewById(R.id.tvActionTwitter);
        itemTwitter = view.findViewById(R.id.itemTwitter);
        itemTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvActionGoogle.getText().equals(getString(R.string.action_connect)))
                    listener.onSocialBindClick(AuthManager.PROVIDER_TWITTER);
                else listener.onSocialUnbindClick(AuthManager.PROVIDER_TWITTER);
            }
        });

        rvEmails = view.findViewById(R.id.rvEmails);
        rvEmails.setLayoutManager(new LinearLayoutManager(getActivity()));
        emailAdapter = new EmailAdapter(this);
        rvEmails.setAdapter(emailAdapter);
        btnAddEmail = view.findViewById(R.id.btnAddEmail);
        btnAddEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddEmailDialogFragment.newInstance(XabberAccountInfoFragment.this)
                        .show(getFragmentManager(), AccountSyncDialogFragment.class.getSimpleName());
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
        else listener.needXMPPAuthFragment();
        updateLastSyncTime();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    @Override
    public void onAddEmailClick(String email) {
        listener.onAddEmailClick(email);
    }

    @Override
    public void onEmailClick(String email) {
        ConfirmEmailDialogFragment.newInstance(this, email)
                .show(getFragmentManager(), AccountSyncDialogFragment.class.getSimpleName());
    }

    @Override
    public void onResendCodeClick(String email) {
        listener.onAddEmailClick(email);
    }

    @Override
    public void onConfirmClick(String email, String code) {
        listener.onConfirmEmailClick(email, code);
    }

    @Override
    public void onSyncClick(boolean needGoToMainActivity) {
        listener.onSyncClick(needGoToMainActivity);
    }

    public void updateData(@NonNull XabberAccount account) {
        String accountName = account.getFirstName() + " " + account.getLastName();
        if (accountName.trim().isEmpty())
            accountName = getString(R.string.title_xabber_account);

        tvAccountName.setText(accountName);
        if (account.getUsername() != null && !account.getUsername().isEmpty())
            tvAccountUsername.setText(account.getUsername());

        if (account.getLanguage() != null && !account.getLanguage().equals("")) {
            tvLanguage.setText(account.getLanguage());
            tvLanguage.setVisibility(View.VISIBLE);
        } else tvLanguage.setVisibility(View.GONE);

        if (BuildConfig.FLAVOR.equals("ru")) {
            tvPhone.setVisibility(View.VISIBLE);
            String phone = account.getPhone();
            tvPhone.setText(phone != null ? phone : getString(R.string.no_phone));
        } else tvPhone.setVisibility(View.GONE);

        setupSocial(account.getSocialBindings());
        setupEmailList(account.getEmails());
    }

    public void updateLastSyncTime() {
        tvLastSyncDate.setText(getString(R.string.last_sync_date, SettingsManager.getLastSyncDate()));
    }

    public void showSyncDialog(final boolean noCancel) {
        Subscription getSettingsSubscription = AuthManager.getClientSettings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<XMPPAccountSettings>>() {
                    @Override
                    public void call(List<XMPPAccountSettings> list) {
                        List<XMPPAccountSettings> items = XabberAccountManager.getInstance().createSyncList(list);

                        if (items != null && items.size() > 0) {
                            // save full list to list for sync
                            XabberAccountManager.getInstance().setXmppAccountsForSync(items);
                            // show dialog
                            AccountSyncDialogFragment.newInstance(XabberAccountInfoFragment.this, noCancel)
                                    .show(getFragmentManager(), AccountSyncDialogFragment.class.getSimpleName());
                            dialogShowed = false;
                        } else Toast.makeText(getActivity(), R.string.sync_fail, Toast.LENGTH_SHORT).show();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        handleErrorGetSettings(throwable);
                    }
                });
        compositeSubscription.add(getSettingsSubscription);
    }

    private void handleErrorGetSettings(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                XabberAccountManager.getInstance().onInvalidToken();
                listener.needXMPPAuthFragment();
                Toast.makeText(getActivity(), R.string.account_deleted, Toast.LENGTH_LONG).show();
            } else {
                Log.d(LOG_TAG, "Error while synchronization: " + message);
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while synchronization: " + throwable.toString());
            Toast.makeText(getActivity(), R.string.sync_fail, Toast.LENGTH_LONG).show();
        }
    }

    private void showLogoutDialog() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_logout_xabber_account, null);
        final CheckBox chbDeleteAccounts = (CheckBox) view.findViewById(R.id.chbDeleteAccounts);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.progress_title_quit)
                .setMessage(R.string.logout_summary)
                .setView(view)
                .setPositiveButton(R.string.button_quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onLogoutClick(chbDeleteAccounts.isChecked());
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        Dialog dialog = builder.create();
        dialog.show();
        dialogShowed = false;
    }

    private void setupSocial(List<SocialBindingDTO> socialBindings) {
        for (SocialBindingDTO socialBinding : socialBindings) {
            if ("google".equals(socialBinding.getProvider())) {
                tvStatusGoogle.setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvActionGoogle.setText(R.string.action_disconnect);
                tvStatusGoogle.setTextColor(getActivity().getResources().getColor(R.color.black_text));
                tvActionGoogle.setTextColor(getActivity().getResources().getColor(R.color.black_text));
            } else if ("facebook".equals(socialBinding.getProvider())) {
                tvStatusFacebook.setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvActionFacebook.setText(R.string.action_disconnect);
                tvStatusFacebook.setTextColor(getActivity().getResources().getColor(R.color.black_text));
                tvActionFacebook.setTextColor(getActivity().getResources().getColor(R.color.black_text));
            } else if ("twitter".equals(socialBinding.getProvider())) {
                tvStatusTwitter.setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvActionTwitter.setText(R.string.action_disconnect);
                tvStatusTwitter.setTextColor(getActivity().getResources().getColor(R.color.black_text));
                tvActionTwitter.setTextColor(getActivity().getResources().getColor(R.color.black_text));
            }
        }
    }


    private void setupEmailList(List<EmailDTO> emails) {
        emailAdapter.setItems(emails);
        emailAdapter.notifyDataSetChanged();
    }
}
