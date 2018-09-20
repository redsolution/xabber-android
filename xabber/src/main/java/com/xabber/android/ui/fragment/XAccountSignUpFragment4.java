package com.xabber.android.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;

public class XAccountSignUpFragment4 extends Fragment {

    public static XAccountSignUpFragment4 newInstance() {
        XAccountSignUpFragment4 fragment = new XAccountSignUpFragment4();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_xaccount_signup_4, container, false);
    }

}
