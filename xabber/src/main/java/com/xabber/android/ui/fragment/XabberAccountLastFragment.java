package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XMPPUser;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.activity.XabberAccountInfoActivity;

import rx.subscriptions.CompositeSubscription;

/**
 * Created by valery.miller on 20.09.17.
 */

public class XabberAccountLastFragment extends Fragment {

    private static final String LOG_TAG = XabberAccountLastFragment.class.getSimpleName();

    private TextView tvAccountName;
    private TextView tvSignType;

    private Button btnYes;
    private Button btnNo;
    private TextView tvDescription;

    private String jid;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xabber_account_register_last, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAccountName = (TextView) view.findViewById(R.id.tvAccountName);
        tvSignType = (TextView) view.findViewById(R.id.tvSignType);

        tvDescription = (TextView) view.findViewById(R.id.tvDescription);
        btnYes = (Button) view.findViewById(R.id.btnYes);
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((XabberAccountInfoActivity)getActivity()).showInfoFragment();
            }
        });

        btnNo = (Button) view.findViewById(R.id.btnNo);
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (jid != null && !jid.isEmpty())
                    ((XabberAccountInfoActivity)getActivity()).onDeleteXabberOrgClick(jid);
                else ((XabberAccountInfoActivity)getActivity()).showLastFragment();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        XabberAccount account = XabberAccountManager.getInstance().getAccount();
        if (account != null) {
            XMPPUser user = account.getXmppUsers().get(0);
            if (user != null) jid = user.getUsername() + "@" + user.getHost();
            tvDescription.setText(getString(R.string.complete_register_summary, jid));
            updateData(account);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    public void updateData(@NonNull XabberAccount account) {
        if (account.getEmails().size() > 0) {
            // if registered via email
            tvAccountName.setText(account.getEmails().get(0).getEmail());
            tvSignType.setText(R.string.signed_up_email);
        } else {
            // if registered via social binding
            String firstName = account.getFirstName();
            String lastName = account.getLastName();
            String name = "";
            if (firstName != null) {
                name = name + firstName;
            }
            if (lastName != null) {
                name = name + " " + lastName;
            }
            if (!name.isEmpty()) tvAccountName.setText(name);

            if (account.getSocialBindings().size() > 0) {
                switch (account.getSocialBindings().get(0).getProvider()) {
                    case "google":
                        tvSignType.setText(R.string.signed_up_google);
                        break;
                    case "facebook":
                        tvSignType.setText(R.string.signed_up_facebook);
                        break;
                    case "twitter":
                        tvSignType.setText(R.string.signed_up_twitter);
                        break;
                    case "github":
                        tvSignType.setText(R.string.signed_up_github);
                        break;
                }
            }
        }

    }
}
