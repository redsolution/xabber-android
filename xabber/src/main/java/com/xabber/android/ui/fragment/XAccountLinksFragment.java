package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.EmailDTO;
import com.xabber.android.data.xaccount.SocialBindingDTO;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.EmailAdapter;
import com.xabber.android.ui.dialog.AddEmailDialogFragment;
import com.xabber.android.ui.dialog.ConfirmEmailDialogFragment;
import com.xabber.android.ui.helper.OnSocialBindListener;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class XAccountLinksFragment  extends Fragment implements EmailAdapter.Listener {

    private ImageView ivGoogle;
    private TextView tvNameGoogle;
    private TextView tvStatusGoogle;
    private TextView tvActionGoogle;

    private ImageView ivTwitter;
    private TextView tvNameTwitter;
    private TextView tvStatusTwitter;
    private TextView tvActionTwitter;

    private EmailAdapter emailAdapter;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private Listener listener;
    private OnSocialBindListener bindListener;

    public interface Listener {
        void onSocialUnbindClick(String provider);
        void onDeleteEmailClick(int emailId);
    }

    public static XAccountLinksFragment newInstance() {
        return new XAccountLinksFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_links, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatusGoogle = view.findViewById(R.id.tvStatusGoogle);
        ivGoogle = view.findViewById(R.id.ivGoogle);
        tvNameGoogle = view.findViewById(R.id.tvNameGoogle);
        tvActionGoogle = view.findViewById(R.id.tvActionGoogle);
        LinearLayout itemGoogle = view.findViewById(R.id.itemGoogle);
        itemGoogle.setOnClickListener(v -> {
            if (tvActionGoogle.getText().equals(getString(R.string.action_connect)))
                bindListener.onBindClick(AuthManager.PROVIDER_GOOGLE);
            else onSocialUnlinkClick(AuthManager.PROVIDER_GOOGLE);
        });

        tvStatusTwitter = view.findViewById(R.id.tvStatusTwitter);
        ivTwitter = view.findViewById(R.id.ivTwitter);
        tvNameTwitter = view.findViewById(R.id.tvNameTwitter);
        tvActionTwitter = view.findViewById(R.id.tvActionTwitter);
        LinearLayout itemTwitter = view.findViewById(R.id.itemTwitter);
        itemTwitter.setOnClickListener(v -> {
            if (tvActionTwitter.getText().equals(getString(R.string.action_connect)))
                bindListener.onBindClick(AuthManager.PROVIDER_TWITTER);
            else onSocialUnlinkClick(AuthManager.PROVIDER_TWITTER);
        });

        RecyclerView rvEmails = view.findViewById(R.id.rvEmails);
        rvEmails.setLayoutManager(new LinearLayoutManager(getActivity()));
        emailAdapter = new EmailAdapter(this);
        rvEmails.setAdapter(emailAdapter);
        View viewAddEmail = view.findViewById(R.id.viewAddEmail);
        viewAddEmail.setOnClickListener(v -> AddEmailDialogFragment.newInstance()
                .show(getFragmentManager(), AddEmailDialogFragment.class.getSimpleName()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) listener = (Listener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement XAccountLinksFragment.Listener");

        if (activity instanceof OnSocialBindListener) bindListener = (OnSocialBindListener) activity;
        else throw new RuntimeException(activity.toString()
                + " must implement OnSocialBindListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        bindListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        compositeSubscription.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeForXabberAccount();
    }

    @Override
    public void onEmailVerifyClick(String email) {
        ConfirmEmailDialogFragment.newInstance(email)
                .show(getFragmentManager(), ConfirmEmailDialogFragment.class.getSimpleName());
    }

    private void onSocialUnlinkClick(final String provider) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.title_delete_social, AuthManager.getProviderName(provider)))
                .setPositiveButton(R.string.action_disconnect, (dialog, which)
                        -> listener.onSocialUnbindClick(provider))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public void onEmailDeleteClick(final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.title_delete_email)
                .setPositiveButton(R.string.action_disconnect, (dialog, which) -> listener.onDeleteEmailClick(id))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void setupSocial(List<SocialBindingDTO> socialBindings) {
        clearSocial();
        for (SocialBindingDTO socialBinding : socialBindings) {
            if ("google".equals(socialBinding.getProvider())) {
                ivGoogle.setImageResource(R.drawable.ic_google_plus);
                tvNameGoogle
                        .setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvNameGoogle.setVisibility(View.VISIBLE);
                tvStatusGoogle.setText(getResources().getString(R.string.title_linked_account,
                        AuthManager.getProviderName(socialBinding.getProvider())));
                tvActionGoogle.setText(R.string.action_disconnect);
                tvActionGoogle.setVisibility(View.GONE);
                tvActionGoogle.setTextColor(getActivity().getResources()
                        .getColor(R.color.account_register_blue));
            } else if ("twitter".equals(socialBinding.getProvider())) {
                ivTwitter.setImageResource(R.drawable.ic_twitter);
                tvNameTwitter
                        .setText(socialBinding.getFirstName() + " " + socialBinding.getLastName());
                tvNameTwitter.setVisibility(View.VISIBLE);
                tvStatusTwitter
                        .setText(getResources().getString(R.string.title_linked_account,
                        AuthManager.getProviderName(socialBinding.getProvider())));
                tvActionTwitter.setText(R.string.action_disconnect);
                tvActionTwitter.setVisibility(View.GONE);
                tvActionTwitter.setTextColor(getActivity().getResources()
                        .getColor(R.color.account_register_blue));
            }
        }
    }

    private void clearSocial() {
        ivGoogle.setImageResource(R.drawable.ic_google_plus_disabled);
        tvNameGoogle.setVisibility(View.GONE);
        tvStatusGoogle.setText(R.string.title_not_linked_account);
        tvActionGoogle.setText(R.string.action_connect);
        tvActionGoogle.setVisibility(View.VISIBLE);
        tvActionGoogle.setTextColor(getActivity().getResources().getColor(R.color.grey_500));

        ivTwitter.setImageResource(R.drawable.ic_twitter_disabled);
        tvNameTwitter.setVisibility(View.GONE);
        tvStatusTwitter.setText(R.string.title_not_linked_account);
        tvActionTwitter.setText(R.string.action_connect);
        tvActionTwitter.setVisibility(View.VISIBLE);
        tvActionTwitter.setTextColor(getActivity().getResources().getColor(R.color.grey_500));
    }

    private void setupEmailList(List<EmailDTO> emails) {
        emailAdapter.setItems(emails);
        emailAdapter.notifyDataSetChanged();
    }

    private void subscribeForXabberAccount() {
        compositeSubscription.add(XabberAccountManager.getInstance().subscribeForAccount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(account -> {
                if (account != null) {
                    setupSocial(account.getSocialBindings());
                    setupEmailList(account.getEmails());
                }
            }).subscribe());
    }

}
