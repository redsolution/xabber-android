package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
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
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }
}
