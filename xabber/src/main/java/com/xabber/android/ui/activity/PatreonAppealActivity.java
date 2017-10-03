package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;

import com.xabber.android.R;
import com.xabber.android.data.http.IXabberCom;
import com.xabber.android.data.http.PatreonManager;
import com.xabber.android.ui.color.BarPainter;

/**
 * Created by valery.miller on 03.10.17.
 */

public class PatreonAppealActivity extends ManagedActivity {

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
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(PatreonAppealActivity.this);
            }
        });
        BarPainter barPainter = new BarPainter(this, toolbar);
        barPainter.setDefaultColor();

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.loadUrl(IXabberCom.APPEAL_URL);
    }
}
