package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.AuthManager;
import com.xabber.android.data.xaccount.EmailDTO;
import com.xabber.android.data.xaccount.SocialBindingDTO;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.adapter.EmailAdapter;
import com.xabber.android.ui.dialog.AccountSyncDialogFragment;
import com.xabber.android.ui.dialog.AddEmailDialogFragment;
import com.xabber.android.ui.dialog.ConfirmEmailDialogFragment;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class XAccountLinksFragment  extends Fragment implements EmailAdapter.Listener {

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

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private Listener listener;

    public interface Listener {
        void onSocialBindClick(String provider);
        void onSocialUnbindClick(String provider);
        void onDeleteEmailClick(int emailId);
    }

    public static XAccountLinksFragment newInstance() {
        XAccountLinksFragment fragment = new XAccountLinksFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_links, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
                AddEmailDialogFragment.newInstance()
                        .show(getFragmentManager(), AccountSyncDialogFragment.class.getSimpleName());
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement XAccountLinksFragment.Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
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
                .show(getFragmentManager(), AccountSyncDialogFragment.class.getSimpleName());
    }

    @Override
    public void onEmailDeleteClick(int id) {
        listener.onDeleteEmailClick(id);
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
    }

    private void clearSocial() {
        ivGoogle.setImageResource(R.drawable.ic_google_plus_disabled);
        tvNameGoogle.setVisibility(View.GONE);
        tvStatusGoogle.setText(R.string.title_not_linked_account);
        tvActionGoogle.setText(R.string.action_connect);
        tvActionGoogle.setTextColor(getActivity().getResources().getColor(R.color.grey_500));

        ivFacebook.setImageResource(R.drawable.ic_facebook_disabled);
        tvNameFacebook.setVisibility(View.GONE);
        tvStatusFacebook.setText(R.string.title_not_linked_account);
        tvActionFacebook.setText(R.string.action_connect);
        tvActionFacebook.setTextColor(getActivity().getResources().getColor(R.color.grey_500));

        ivTwitter.setImageResource(R.drawable.ic_twitter_disabled);
        tvNameTwitter.setVisibility(View.GONE);
        tvStatusTwitter.setText(R.string.title_not_linked_account);
        tvActionTwitter.setText(R.string.action_connect);
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
            .doOnNext(new Action1<XabberAccount>() {
                @Override
                public void call(XabberAccount account) {
                    if (account != null) {
                        setupSocial(account.getSocialBindings());
                        setupEmailList(account.getEmails());
                    }
                }
            }).subscribe());
    }

}
