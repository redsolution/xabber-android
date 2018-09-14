package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.BuildConfig;
import com.xabber.android.R;
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

    private ImageView ivGoogle;
    private TextView tvNameGoogle;
    private TextView tvStatusGoogle;
    private TextView tvActionGoogle;
    private LinearLayout itemGoogle;
    private ImageView ivFacebook;
    private TextView tvNameFacebook;
    private TextView tvStatusFacebook;
    private TextView tvActionFacebook;
    private LinearLayout itemFacebook;
    private ImageView ivTwitter;
    private TextView tvNameTwitter;
    private TextView tvStatusTwitter;
    private TextView tvActionTwitter;
    private LinearLayout itemTwitter;

    private RecyclerView rvEmails;
    private EmailAdapter emailAdapter;
    private View viewAddEmail;

    private TextView tvLinks;
    private View viewShowLinks;
    private View viewLinks;
    private ImageView ivChevron;

    private Fragment fragmentSync;
    private FragmentTransaction fTrans;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private Listener listener;

    public interface Listener {
        void onSocialBindClick(String provider);
        void onSocialUnbindClick(String provider);
        void onAddEmailClick(String email);
        void onConfirmEmailClick(String email, String code);
        void onLogoutClick(boolean deleteAccounts);
        void onSyncClick(boolean needGoToMainActivity);
    }

    public static XabberAccountInfoFragment newInstance(Listener listener) {
        XabberAccountInfoFragment fragment = new XabberAccountInfoFragment();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_info, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPhone = (TextView) view.findViewById(R.id.tvPhoneNumber);
        tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        tvAccountUsername = (TextView) view.findViewById(R.id.tvAccountUsername);
        tvLanguage = (TextView) view.findViewById(R.id.tvLanguage);

        ivChevron = view.findViewById(R.id.ivChevron);
        tvLinks = view.findViewById(R.id.tvLinks);
        viewLinks = view.findViewById(R.id.viewLinks);
        viewShowLinks = view.findViewById(R.id.viewShowLinks);
        viewShowLinks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewLinks.setVisibility(viewLinks.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE );
                ivChevron.setImageResource(viewLinks.getVisibility() == View.VISIBLE
                        ? R.drawable.ic_chevron_up : R.drawable.ic_chevron_down);
            }
        });

        tvStatusGoogle = view.findViewById(R.id.tvStatusGoogle);
        ivGoogle = view.findViewById(R.id.ivGoogle);
        tvNameGoogle = view.findViewById(R.id.tvNameGoogle);
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
        ivFacebook = view.findViewById(R.id.ivFacebook);
        tvNameFacebook = view.findViewById(R.id.tvNameFacebook);
        tvActionFacebook = view.findViewById(R.id.tvActionFacebook);
        itemFacebook = view.findViewById(R.id.itemFacebook);
        itemFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvActionFacebook.getText().equals(getString(R.string.action_connect)))
                    listener.onSocialBindClick(AuthManager.PROVIDER_FACEBOOK);
                else listener.onSocialUnbindClick(AuthManager.PROVIDER_FACEBOOK);
            }
        });

        tvStatusTwitter = view.findViewById(R.id.tvStatusTwitter);
        ivTwitter = view.findViewById(R.id.ivTwitter);
        tvNameTwitter = view.findViewById(R.id.tvNameTwitter);
        tvActionTwitter = view.findViewById(R.id.tvActionTwitter);
        itemTwitter = view.findViewById(R.id.itemTwitter);
        itemTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tvActionTwitter.getText().equals(getString(R.string.action_connect)))
                    listener.onSocialBindClick(AuthManager.PROVIDER_TWITTER);
                else listener.onSocialUnbindClick(AuthManager.PROVIDER_TWITTER);
            }
        });

        rvEmails = view.findViewById(R.id.rvEmails);
        rvEmails.setLayoutManager(new LinearLayoutManager(getActivity()));
        emailAdapter = new EmailAdapter(this);
        rvEmails.setAdapter(emailAdapter);
        viewAddEmail = view.findViewById(R.id.viewAddEmail);
        viewAddEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddEmailDialogFragment.newInstance(XabberAccountInfoFragment.this)
                        .show(getFragmentManager(), AccountSyncDialogFragment.class.getSimpleName());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            updateData(account);
        }
        else getActivity().finish();

        getSettings();
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
        String accountName = account.getFullUsername();

        tvAccountName.setText(accountName);
        tvAccountUsername.setText("Free account");

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
        if (fragmentSync != null && fragmentSync.isVisible())
            ((AccountSyncFragment) fragmentSync).updateLastSyncTime();
    }

    private void getSettings() {
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
                            // show fragment
                            showSyncFragment();
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

    private void showSyncFragment() {
        fragmentSync = AccountSyncFragment.newInstance();
        fTrans = getFragmentManager().beginTransaction();
        fTrans.replace(R.id.childContainer, fragmentSync);
        fTrans.commit();
    }

    private void handleErrorGetSettings(Throwable throwable) {
        String message = RetrofitErrorConverter.throwableToHttpError(throwable);
        if (message != null) {
            if (message.equals("Invalid token")) {
                XabberAccountManager.getInstance().onInvalidToken();
                Toast.makeText(getActivity(), R.string.account_deleted, Toast.LENGTH_LONG).show();
                getActivity().finish();
            } else {
                Log.d(LOG_TAG, "Error while synchronization: " + message);
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Error while synchronization: " + throwable.toString());
            Toast.makeText(getActivity(), R.string.sync_fail, Toast.LENGTH_LONG).show();
        }
    }

    private void setupSocial(List<SocialBindingDTO> socialBindings) {
        clearSocial();
        for (SocialBindingDTO socialBinding : socialBindings) {
            if ("google".equals(socialBinding.getProvider())) {
                ivGoogle.setImageResource(R.drawable.ic_google_plus);
                tvNameGoogle.setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvNameGoogle.setVisibility(View.VISIBLE);
                tvStatusGoogle.setText(getResources().getString(R.string.title_linked_account, "Google +"));
                tvActionGoogle.setText(R.string.action_disconnect);
                tvActionGoogle.setTextColor(getActivity().getResources().getColor(R.color.account_register_blue));
            } else if ("facebook".equals(socialBinding.getProvider())) {
                ivFacebook.setImageResource(R.drawable.ic_facebook);
                tvNameFacebook.setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvNameFacebook.setVisibility(View.VISIBLE);
                tvStatusFacebook.setText(getResources().getString(R.string.title_linked_account, "Facebook"));
                tvActionFacebook.setText(R.string.action_disconnect);
                tvActionFacebook.setTextColor(getActivity().getResources().getColor(R.color.account_register_blue));
            } else if ("twitter".equals(socialBinding.getProvider())) {
                ivTwitter.setImageResource(R.drawable.ic_twitter);
                tvNameTwitter.setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvNameTwitter.setVisibility(View.VISIBLE);
                tvStatusTwitter.setText(getResources().getString(R.string.title_linked_account, "Twitter"));
                tvActionTwitter.setText(R.string.action_disconnect);
                tvActionTwitter.setTextColor(getActivity().getResources().getColor(R.color.account_register_blue));
            }
        }
        if (socialBindings.size() > 0) tvLinks.setText(R.string.title_linked_accounts);
    }

    private void clearSocial() {
        ivGoogle.setImageResource(R.drawable.ic_google_plus_disabled);
        tvNameGoogle.setVisibility(View.GONE);
        tvStatusGoogle.setText(R.string.title_not_linked_account);
        tvActionGoogle.setText(R.string.action_connect);

        ivFacebook.setImageResource(R.drawable.ic_google_plus_disabled);
        tvNameFacebook.setVisibility(View.GONE);
        tvStatusFacebook.setText(R.string.title_not_linked_account);
        tvActionFacebook.setText(R.string.action_connect);

        ivTwitter.setImageResource(R.drawable.ic_google_plus_disabled);
        tvNameTwitter.setVisibility(View.GONE);
        tvStatusTwitter.setText(R.string.title_not_linked_account);
        tvActionTwitter.setText(R.string.action_connect);
    }

    private void setupEmailList(List<EmailDTO> emails) {
        if (emails.size() > 0) tvLinks.setText(R.string.title_linked_accounts);
        emailAdapter.setItems(emails);
        emailAdapter.notifyDataSetChanged();
    }
}
