package com.xabber.android.ui.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;


public class IntroActivity extends ManagedActivity {

    public static Intent createIntent(Context context) {
        return new Intent(context, IntroActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_intro);

        ((TextView) findViewById(R.id.intro_faq_text))
                .setMovementMethod(LinkMovementMethod.getInstance());

        Button haveAccountButton = (Button) findViewById(R.id.intro_have_account_button);
        Button registerAccountButton = (Button) findViewById(R.id.intro_register_account_button);
        Button createXabberAccountButton = (Button) findViewById(R.id.intro_create_xabber_account_button);


        haveAccountButton.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
        registerAccountButton.getBackground().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
        createXabberAccountButton.getBackground().setColorFilter(getResources().getColor(R.color.red_700), PorterDuff.Mode.MULTIPLY);

        haveAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(AccountAddActivity.createIntent(IntroActivity.this));
            }
        });

        findViewById(R.id.intro_register_account_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchQuery = getString(R.string.intro_web_search_register_xmpp);
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
                intent.putExtra(SearchManager.QUERY, searchQuery);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AccountManager.getInstance().hasAccounts()) {
            finish();
            startActivity(ContactListActivity.createIntent(this));
        }
    }
}
