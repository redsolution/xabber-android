package com.xabber.android.ui.dialog;

import android.app.DialogFragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomInvite;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.activity.ConferenceAdd;
import com.xabber.xmpp.address.Jid;

public class MucInviteDialog extends BaseContactDialog {

    private RoomInvite roomInvite;

    public static DialogFragment newInstance(String account, String contact) {
        DialogFragment fragment = new MucInviteDialog();
        setArguments(account, contact, fragment);
        return fragment;
    }

    @Override
    protected int getDialogTitleTextResource() {
        return R.string.conference_invitation;
    }

    @Override
    protected String getMessage() {
        return roomInvite.getConfirmation();
    }

    @Override
    protected int getNegativeButtonTextResource() {
        return android.R.string.cancel;
    }

    @Override
    protected int getPositiveButtonTextResource() {
        return R.string.muc_join;
    }

    @Override
    protected Integer getNeutralButtonTextResourceOrNull() {
        return null;
    }

    @Override
    protected void onPositiveButtonClick() {
        startActivity(ConferenceAdd.createIntent(getActivity(), getAccount(), getContact()));
    }

    @Override
    protected void onNegativeButtonClick() {
        MUCManager.getInstance().removeInvite(roomInvite);
    }

    @Override
    protected void onNeutralButtonClick() {

    }

    @Override
    protected void setUpContactTitleView(View view) {
        roomInvite = MUCManager.getInstance().getInvite(getAccount(), getContact());
        final String inviter = Jid.getBareAddress(roomInvite.getInviter());

        final AbstractContact bestContact = RosterManager.getInstance().getBestContact(getAccount(), inviter);

        ((ImageView)view.findViewById(R.id.avatar)).setImageDrawable(bestContact.getAvatar());
        ((TextView)view.findViewById(R.id.name)).setText(bestContact.getName());
        ((TextView)view.findViewById(R.id.status_text)).setText(inviter);
    }

}
