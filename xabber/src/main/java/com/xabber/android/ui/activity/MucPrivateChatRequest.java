package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.fragment.ContactVcardViewerFragment;
import com.xabber.android.ui.helper.AccountPainter;
import com.xabber.android.ui.helper.BarPainter;
import com.xabber.android.ui.helper.SingleActivity;
import com.xabber.xmpp.address.Jid;

public class MucPrivateChatRequest extends SingleActivity implements View.OnClickListener, ContactVcardViewerFragment.Listener {

    private String account;
    private String user;

    public static Intent createIntent(Context context, String account, String user) {
        return new EntityIntentBuilder(context, MucPrivateChatRequest.class)
                .setAccount(account).setUser(user).build();
    }

    private static String getAccount(Intent intent) {
        return EntityIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        account = getAccount(intent);
        user = getUser(intent);

        setContentView(R.layout.contact_subscription);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setTitle(R.string.conference_private_chat);
        toolbar.setNavigationIcon(R.drawable.ic_clear_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);
        AccountPainter accountPainter = new AccountPainter(this);

        View fakeToolbar = findViewById(R.id.fake_toolbar);

        fakeToolbar.setBackgroundColor(accountPainter.getAccountMainColor(account));

        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);

        ((ImageView)fakeToolbar.findViewById(R.id.avatar)).setImageDrawable(abstractContact.getAvatar());

        ((TextView)fakeToolbar.findViewById(R.id.dialog_message)).setText(String.format(getString(R.string.conference_private_chat_invitation), Jid.getResource(user), Jid.getBareAddress(user)));

        Button acceptButton = (Button) findViewById(R.id.accept_button);
        acceptButton.setText(R.string.accept_muc_private_chat);
        acceptButton.setTextColor(accountPainter.getAccountMainColor(account));
        acceptButton.setOnClickListener(this);

        findViewById(R.id.decline_button).setOnClickListener(this);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.scrollable_container, ContactVcardViewerFragment.newInstance(account, user)).commit();
        }


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.accept_button:
                onAccept();
                break;

            case R.id.decline_button:
                onDecline();
                break;
        }
    }

    public void onAccept() {
        MessageManager.getInstance().acceptMucPrivateChat(account, user);
        startActivity(ChatViewer.createSpecificChatIntent(Application.getInstance(), account, user));
        finish();
    }

    public void onDecline() {
        MessageManager.getInstance().discardMucPrivateChat(account, user);
        finish();
    }

    @Override
    public void onVCardReceived() {

    }
}
