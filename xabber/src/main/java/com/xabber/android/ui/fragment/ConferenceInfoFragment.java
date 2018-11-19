package com.xabber.android.ui.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.extension.muc.MUCManager;

import org.jivesoftware.smackx.muc.RoomInfo;
import org.jxmpp.jid.EntityBareJid;

public class ConferenceInfoFragment extends Fragment implements MUCManager.RoomInfoListener {
    public static final String ARGUMENT_ACCOUNT = "com.xabber.android.ui.fragment.ConferenceInfoFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_ROOM = "com.xabber.android.ui.fragment.ConferenceInfoFragment.ARGUMENT_ROOM";
    public static final String SAVE_IS_LOADED = "com.xabber.android.ui.fragment.ConferenceInfoFragment.SAVE_IS_LOADED";

    private AccountJid account;
    private EntityBareJid room;

    private TextView jidTextView;
    private TextView nameTextView;
    private TextView descriptionTextView;
    private TextView subjectTextView;
    private TextView occupantsTextView;
    private View nameView;
    private View descriptionView;
    private View subjectView;
    private View occupantsView;
    private View jidView;
    private View progressBar;

    private boolean isInfoLoaded;

    public static ConferenceInfoFragment newInstance(AccountJid account, EntityBareJid room) {
        ConferenceInfoFragment fragment = new ConferenceInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_ACCOUNT, account);
        args.putSerializable(ARGUMENT_ROOM, room);
        fragment.setArguments(args);
        return fragment;
    }

    public ConferenceInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            account = getArguments().getParcelable(ARGUMENT_ACCOUNT);
            room = (EntityBareJid) getArguments().getSerializable(ARGUMENT_ROOM);
        }

        if (savedInstanceState != null) {
            isInfoLoaded = savedInstanceState.getBoolean(SAVE_IS_LOADED, false);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_conference_info, container, false);

        jidTextView = (TextView) view.findViewById(R.id.conference_info_jid_text);
        jidView = view.findViewById(R.id.conference_info_jid);


        nameTextView = (TextView) view.findViewById(R.id.conference_info_name_text);
        nameView = view.findViewById(R.id.conference_info_name);



        descriptionTextView = (TextView) view.findViewById(R.id.conference_info_description_text);
        descriptionView = view.findViewById(R.id.conference_info_description);



        subjectTextView = (TextView) view.findViewById(R.id.conference_info_subject_text);
        subjectView = view.findViewById(R.id.conference_info_subject);


        occupantsTextView = (TextView) view.findViewById(R.id.conference_info_occupants_text);
        occupantsView = view.findViewById(R.id.conference_info_occupants);



        progressBar = view.findViewById(R.id.conference_info_progress_bar);
        progressBar.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (jidTextView.length() == 0) {
            jidView.setVisibility(View.GONE);
        }
        if (nameTextView.length() == 0) {
            nameView.setVisibility(View.GONE);
        }
        if (descriptionTextView.length() == 0) {
            descriptionView.setVisibility(View.GONE);
        }
        if (subjectTextView.length() == 0) {
            subjectView.setVisibility(View.GONE);
        }
        if (occupantsTextView.length() == 0) {
            occupantsView.setVisibility(View.GONE);
        }



        if (!isInfoLoaded) {
            jidView.setVisibility(View.GONE);
            nameView.setVisibility(View.GONE);
            descriptionView.setVisibility(View.GONE);
            subjectView.setVisibility(View.GONE);
            occupantsView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);


            progressBar.setVisibility(View.VISIBLE);
            MUCManager.requestRoomInfo(account, room, this);
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_IS_LOADED, isInfoLoaded);
    }

    @Override
    public void onRoomInfoReceived(RoomInfo roomInfo) {
        if (!isAdded()) {
            return;
        }

        progressBar.setVisibility(View.GONE);

        if (roomInfo == null) {
            Toast.makeText(getActivity(), getString(R.string.could_not_get_room_info), Toast.LENGTH_SHORT).show();
            return;
        }

        isInfoLoaded = true;


        if (!"".equals(roomInfo.getRoom())) {
            jidView.setVisibility(View.VISIBLE);
            jidTextView.setText(roomInfo.getRoom());
        } else {
            jidTextView.setText(null);
        }
        if (!"".equals(roomInfo.getName())) {
            nameView.setVisibility(View.VISIBLE);
            nameTextView.setText(roomInfo.getName());
        } else {
            nameTextView.setText(null);
        }
        if (!"".equals(roomInfo.getDescription())) {
            descriptionView.setVisibility(View.VISIBLE);
            descriptionTextView.setText(roomInfo.getDescription());
        } else {
            descriptionTextView.setText(null);
        }
        if (!"".equals(roomInfo.getSubject())) {
            subjectView.setVisibility(View.VISIBLE);
            subjectTextView.setText(roomInfo.getSubject());
        } else {
            subjectTextView.setText(null);
        }
        if (roomInfo.getOccupantsCount() != -1) {
            occupantsView.setVisibility(View.VISIBLE);
            occupantsTextView.setText(String.format(getString(R.string.message_info_occupants_number), roomInfo.getOccupantsCount()));
        } else {
            occupantsTextView.setText(null);
        }

    }
}
