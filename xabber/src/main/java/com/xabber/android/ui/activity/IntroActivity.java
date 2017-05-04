package com.xabber.android.ui.activity;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.log.LogManager;

import java.lang.reflect.InvocationTargetException;


public class IntroActivity extends ManagedActivity {

    private static final String LOG_TAG = IntroActivity.class.getSimpleName();
    private View rootLayout;

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
        setStatusBarTranslucent();

        ImageView backgroundImage = (ImageView) findViewById(R.id.intro_background_image);

        Glide.with(this)
                .load(R.drawable.intro_background)
                .centerCrop()
                .into(backgroundImage);

        ImageView logoImage = (ImageView) findViewById(R.id.intro_logo_image);

        Glide.with(this)
                .load(R.drawable.xabber_logo_large)
                .into(logoImage);

        ((TextView) findViewById(R.id.intro_faq_text))
                .setMovementMethod(LinkMovementMethod.getInstance());

        Button haveAccountButton = (Button) findViewById(R.id.intro_have_account_button);
        Button registerAccountButton = (Button) findViewById(R.id.intro_register_account_button);
        Button createXabberAccountButton = (Button) findViewById(R.id.intro_create_xabber_account_button);

        createXabberAccountButton.setVisibility(View.GONE);

        haveAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(AccountAddActivity.createIntent(IntroActivity.this));
            }
        });

        registerAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchHowToRegister();
            }
        });
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

    void setStatusBarTranslucent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (AccountManager.getInstance().hasAccounts()) {
            Intent intent = ContactListActivity.createIntent(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            finish();
            startActivity(intent);
        }
    }

}
