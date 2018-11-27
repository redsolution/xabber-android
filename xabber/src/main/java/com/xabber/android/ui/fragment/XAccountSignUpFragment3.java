package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.xabber.android.R;

public class XAccountSignUpFragment3 extends Fragment {

    private Listener listener;

    private Button btnSignUp;
    private CheckBox chkAccept;

    public interface Listener {
        void onStep3Completed();
    }

    public static XAccountSignUpFragment3 newInstance(XAccountSignUpFragment3.Listener listener) {
        XAccountSignUpFragment3 fragment = new XAccountSignUpFragment3();
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_signup_3, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSignUp = view.findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onStep3Completed();
            }
        });

        chkAccept = view.findViewById(R.id.chkAccept);
        chkAccept.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                btnSignUp.setEnabled(isChecked);
            }
        });

        WebView webView = view.findViewById(R.id.webView);
        webView.loadUrl(getString(R.string.license_url));
    }

}
