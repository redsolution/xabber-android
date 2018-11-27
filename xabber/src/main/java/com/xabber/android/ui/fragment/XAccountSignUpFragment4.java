package com.xabber.android.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
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
    private Dialog skipDialog;

    private boolean haveLinks = false;

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
                onStartClick();
            }
        });

        Fragment linksFragment = XAccountLinksFragment.newInstance();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.childContainer, linksFragment).commit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof Listener) listener = (Listener) activity;
        else throw new RuntimeException(activity.toString()
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
                    if (account != null) {
                        if (account.getSocialBindings().size() > 0 || account.getEmails().size() > 0)
                            haveLinks = true;
                        else haveLinks = false;
                        setupStartButton();
                    }
                }
            }).subscribe());
    }

    private void setupStartButton() {
        btnStart.setText(haveLinks ? R.string.title_start_messaging : R.string.skip);
        btnStart.getBackground().setColorFilter(ContextCompat.getColor(getActivity(),
                haveLinks ? R.color.account_register_blue : R.color.grey_400),
                PorterDuff.Mode.MULTIPLY);
    }

    private void onStartClick() {
        if (haveLinks) listener.onStep4Completed();
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            skipDialog = builder.setTitle(R.string.warning)
                    .setMessage(R.string.account_secure_warning)
                    .setView(setupDialogView()).create();
            skipDialog.show();
        }
    }

    private View setupDialogView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.skip_links_dialog, null);

        Button btnSkip = view.findViewById(R.id.btnSkip);
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onStep4Completed();
            }
        });

        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeDialog();
            }
        });

        return view;
    }

    private void closeDialog() {
        if (skipDialog != null && skipDialog.isShowing())
            skipDialog.dismiss();
    }
}
