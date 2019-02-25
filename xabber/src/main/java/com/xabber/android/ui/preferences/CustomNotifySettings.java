package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.color.BarPainter;

public class CustomNotifySettings extends ManagedActivity {

    private final static String GROUP_KEY = "group";
    private final static String PHRASE_ID_KEY = "phraseID";

    private AccountJid account;
    private UserJid user;
    private String group;
    private Long phraseID;

    public static Intent createIntent(Context context, AccountJid account) {
        return new EntityIntentBuilder(context, CustomNotifySettings.class).setAccount(account).build();
    }

    public static Intent createIntent(Context context, AccountJid account, UserJid user) {
        return new EntityIntentBuilder(context, CustomNotifySettings.class).setAccount(account).setUser(user).build();
    }

    public static Intent createIntent(Context context, AccountJid account, String group) {
        Intent intent = new EntityIntentBuilder(context, CustomNotifySettings.class).setAccount(account).build();
        intent.putExtra(GROUP_KEY, group);
        return intent;
    }

    public static Intent createIntent(Context context, Long phraseID) {
        Intent intent = new Intent(context, CustomNotifySettings.class);
        intent.putExtra(PHRASE_ID_KEY, phraseID);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_toolbar_and_container);

        account = EntityIntentBuilder.getAccount(getIntent());
        user = EntityIntentBuilder.getUser(getIntent());
        group = getIntent().getStringExtra(GROUP_KEY);
        phraseID = getIntent().getLongExtra(PHRASE_ID_KEY, -1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(CustomNotifySettings.this);
            }
        });

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(account);

        if (savedInstanceState == null) {
            Key key = Key.createKey(account, user, group, phraseID);
            if (key == null) finish();
            getFragmentManager().beginTransaction().add(R.id.fragment_container,
                    CustomNotifSettingsFragment.createInstance(this, key)).commit();
        }
    }
}
