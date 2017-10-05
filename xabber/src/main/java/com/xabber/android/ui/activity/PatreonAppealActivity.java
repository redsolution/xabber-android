package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.xabber.android.R;
import com.xabber.android.data.http.IXabberCom;
import com.xabber.android.data.http.PatreonManager;

/**
 * Created by valery.miller on 03.10.17.
 */

public class PatreonAppealActivity extends ManagedActivity implements Toolbar.OnMenuItemClickListener {

    public static Intent createIntent(Context context) {
        PatreonManager.getInstance().loadFromNet();
        return new Intent(context, PatreonAppealActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patreon_appeal);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setTitle(R.string.patreon_title);

        toolbar.inflateMenu(R.menu.toolbar_patreon_activity);
        toolbar.setOnMenuItemClickListener(this);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(PatreonAppealActivity.this);
            }
        });

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.loadUrl(getString(R.string.patreon_appeal_url));
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                startActivity(Intent.createChooser(createShareIntent(), getString(R.string.send_to)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Intent createShareIntent() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = IXabberCom.SHARE_URL;
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        return sharingIntent;
    }
}
