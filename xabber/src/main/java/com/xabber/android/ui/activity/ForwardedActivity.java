package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.MessageDatabaseManager;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.adapter.chat.ForwardedAdapter;
import com.xabber.android.ui.adapter.chat.MessagesAdapter;
import com.xabber.android.ui.color.ColorManager;

import io.realm.RealmResults;

public class ForwardedActivity extends FileInteractionActivity implements ForwardedAdapter.ForwardListener {

    private String messageId;
    private UserJid user;
    private AccountJid account;
    private String userName;
    private int accountMainColor;
    private ColorStateList colorStateList;
    private boolean isMUC;

    private RecyclerView recyclerView;
    private View backgroundView;
    private Toolbar toolbar;

    private final static String KEY_MESSAGE_ID = "messageId";
    private final static String KEY_ACCOUNT = "account";
    private final static String KEY_USER = "user";

    public static Intent createIntent(Context context, String messageId, UserJid user, AccountJid account) {
        Intent intent = new Intent(context, ForwardedActivity.class);
        intent.putExtra(KEY_MESSAGE_ID, messageId);
        intent.putExtra(KEY_ACCOUNT, (Parcelable) account);
        intent.putExtra(KEY_USER, user);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forwarded);

        Intent intent = getIntent();
        if (intent != null) {
            messageId = intent.getStringExtra(KEY_MESSAGE_ID);
            account = intent.getParcelableExtra(KEY_ACCOUNT);
            user = intent.getParcelableExtra(KEY_USER);

            userName = RosterManager.getInstance().getName(account, user);
            accountMainColor = ColorManager.getInstance().getAccountPainter().getAccountMainColor(account);
            colorStateList = ColorManager.getInstance().getChatIncomingBalloonColorsStateList(account);
            isMUC = MUCManager.getInstance().hasRoom(account, user.getJid().asEntityBareJidIfPossible());
        }

        recyclerView = findViewById(R.id.recyclerView);
        backgroundView = findViewById(R.id.backgroundView);

        toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setBackgroundColor(ColorManager.getInstance().getAccountPainter().getAccountMainColor(account));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(ForwardedActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // background
        if (SettingsManager.chatsShowBackground()) {
            if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.dark) {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat_dark);
            } else {
                backgroundView.setBackgroundResource(R.drawable.chat_background_repeat);
            }
        } else {
            backgroundView.setBackgroundColor(ColorManager.getInstance().getChatBackgroundColor());
        }

        // messages adapter
        MessageItem messageItem = MessageDatabaseManager.getInstance().getRealmUiThread().where(MessageItem.class)
                .equalTo(MessageItem.Fields.UNIQUE_ID, messageId).findFirst();

        RealmResults<MessageItem> forwardedMessages =
                MessageDatabaseManager.getInstance().getRealmUiThread().where(MessageItem.class)
                        .in(MessageItem.Fields.UNIQUE_ID, messageItem.getForwardedIdsAsArray()).findAll();

        MessagesAdapter.MessageExtraData extraData = new MessagesAdapter.MessageExtraData(this, this, this,
                userName, colorStateList, accountMainColor, isMUC, false, false,
                false, false);

        if (forwardedMessages.size() > 0) {
            ForwardedAdapter adapter = new ForwardedAdapter(forwardedMessages, extraData);
            recyclerView.setLayoutManager(new LinearLayoutManager(extraData.getContext()));
            recyclerView.setAdapter(adapter);
            toolbar.setTitle(String.format(getString(R.string.forwarded_messages_count), forwardedMessages.size()));

        }
    }

    /** Forwards listener */

    @Override
    public void onForwardClick(String messageId) {
        startActivity(ForwardedActivity.createIntent(this, messageId, user, account));
    }
}
