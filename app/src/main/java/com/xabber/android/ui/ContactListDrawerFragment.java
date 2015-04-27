package com.xabber.android.ui;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.androiddev.R;

public class ContactListDrawerFragment extends Fragment implements View.OnClickListener {

    ContactListDrawerListener listener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_list_drawer, container, false);

        view.findViewById(R.id.drawer_action_settings).setOnClickListener(this);
        view.findViewById(R.id.drawer_action_about).setOnClickListener(this);
        view.findViewById(R.id.drawer_action_exit).setOnClickListener(this);

        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (ContactListDrawerListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onClick(View v) {
        listener.onContactListDrawerListener(v.getId());

    }

    interface ContactListDrawerListener {
        void onContactListDrawerListener(int viewId);
    }
}
