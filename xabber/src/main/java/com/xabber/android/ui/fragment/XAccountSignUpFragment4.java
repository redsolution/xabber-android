package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.xaccount.XabberAccount;
import com.xabber.android.data.xaccount.XabberAccountManager;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class XAccountSignUpFragment4 extends Fragment {

    private Button btnStart;
    private Listener listener;
    private TextView tvDescription;

    public interface Listener {
        void onStep4Completed();
    }

    private CompositeSubscription compositeSubscription = new CompositeSubscription();

    public static XAccountSignUpFragment4 newInstance() {
        XAccountSignUpFragment4 fragment = new XAccountSignUpFragment4();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_signup_4, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        tvDescription = view.findViewById(R.id.tvDescription);
        tvDescription.setText(Html.fromHtml(getActivity().getString(R.string.account_secure_description)));
        btnStart = view.findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onStep4Completed();
            }
        });

        Fragment linksFragment = XAccountLinksFragment.newInstance();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.childContainer, linksFragment).commit();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Listener) listener = (Listener) context;
        else throw new RuntimeException(context.toString()
                    + " must implement XAccountSignUpFragment4.Listener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        subscribeForXabberAccount();
    }

    @Override
    public void onPause() {
        super.onPause();
        compositeSubscription.clear();
    }

    private void subscribeForXabberAccount() {
        compositeSubscription.add(XabberAccountManager.getInstance().subscribeForAccount()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(new Action1<XabberAccount>() {
                @Override
                public void call(XabberAccount account) {
                    if (account.getSocialBindings().size() > 0 || account.getEmails().size() > 0)
                        btnStart.setText(R.string.title_start_messaging);
                    else btnStart.setText(R.string.skip);
                }
            }).subscribe());
    }
}
