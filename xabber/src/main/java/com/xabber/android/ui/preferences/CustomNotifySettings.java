package com.xabber.android.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import com.xabber.android.R;
import com.xabber.android.data.IntentHelpersKt;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.notification.custom_notification.Key;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.color.BarPainter;

public class CustomNotifySettings extends ManagedActivity {

    private final static String GROUP_KEY = "group";
    private final static String PHRASE_ID_KEY = "phraseID";

    private AccountJid account;
    private ContactJid user;
    private String group;
    private Long phraseID;

    public static Intent createIntent(Context context, AccountJid account) {
        return IntentHelpersKt.createAccountIntent(context, CustomNotifySettings.class, account);
    }

    public static Intent createIntent(Context context, AccountJid account, ContactJid user) {
        return IntentHelpersKt.createContactIntent(
                context, CustomNotifySettings.class, account, user
        );
    }

    public static Intent createIntent(Context context, AccountJid account, String group) {
        Intent intent = createIntent(context, account);
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

        account = IntentHelpersKt.getAccountJid(getIntent());
        user = IntentHelpersKt.getContactJid(getIntent());
        group = getIntent().getStringExtra(GROUP_KEY);
        phraseID = getIntent().getLongExtra(PHRASE_ID_KEY, -1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setTitle(getTitle());
        if (SettingsManager.interfaceTheme() == SettingsManager.InterfaceTheme.light)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_left_grey_24dp);
        else toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(CustomNotifySettings.this);
            }
        });

        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.updateWithAccountName(AccountManager.INSTANCE.getFirstAccount());

        if (savedInstanceState == null) {
            Key key = Key.createKey(account, user, group, phraseID);
            if (key == null) finish();
            getFragmentManager().beginTransaction().add(R.id.content_container,
                    CustomNotifSettingsFragment.createInstance(this, key)).commit();
        }
    }
}
