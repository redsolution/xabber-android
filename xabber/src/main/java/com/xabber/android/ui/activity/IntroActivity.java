package com.xabber.android.ui.activity;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.xaccount.XabberAccountManager;

public class IntroActivity extends ManagedActivity {

    private static final String LOG_TAG = IntroActivity.class.getSimpleName();

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

//        ((TextView) findViewById(R.id.intro_faq_text))
//                .setMovementMethod(LinkMovementMethod.getInstance());

        Button btnBasicXmpp = (Button) findViewById(R.id.btnBasicXmpp);
        Button btnLoginXabber = (Button) findViewById(R.id.btnLoginXabber);

        btnLoginXabber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(TutorialActivity.createIntent(IntroActivity.this));
            }
        });

        btnBasicXmpp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(AccountAddActivity.createIntent(IntroActivity.this));
            }
        });

//        registerAccountButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                searchHowToRegister();
//            }
//        });
    }

    private void searchHowToRegister() {
        String searchQuery = getString(R.string.intro_web_search_register_xmpp);

        if (!startWebSearchActivity(searchQuery)) {
            if (!startSearchGoogleActivity(searchQuery)) {
                LogManager.w(LOG_TAG, "Could not find web search or browser activity");
            }
        }
    }

    private boolean startSearchGoogleActivity(String searchQuery) {
        Uri uri = Uri.parse("http://www.google.com/#q=" + searchQuery);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }

    }

    private boolean startWebSearchActivity(String searchQuery) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
        intent.putExtra(SearchManager.QUERY, searchQuery);
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            LogManager.exception(LOG_TAG, e);
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AccountManager.getInstance().hasAccounts() || XabberAccountManager.getInstance().getAccount() != null) {
            Intent intent = ContactListActivity.createIntent(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            finish();
            startActivity(intent);
        }
    }

}
