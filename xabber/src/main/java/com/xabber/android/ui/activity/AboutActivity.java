/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * <p/>
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * <p/>
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.xabber.android.R;
import com.xabber.android.data.log.LogManager;

public class AboutActivity extends ManagedActivity implements View.OnClickListener {

    public static Intent createIntent(Context context) {
        return new Intent(context, AboutActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(AboutActivity.this);
            }
        });

        findViewById(R.id.about_github).setOnClickListener(this);
        findViewById(R.id.about_twitter).setOnClickListener(this);
        findViewById(R.id.about_redsolution).setOnClickListener(this);
        findViewById(R.id.about_text_xmpp_protocol).setOnClickListener(this);

        ((TextView) findViewById(R.id.about_text_developers))
                .setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.about_text_translators))
                .setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.about_text_license))
                .setMovementMethod(LinkMovementMethod.getInstance());

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getString(R.string.application_title_short));

        ((TextView) findViewById(R.id.about_version)).setText(getVersionName());

        loadBackdrop();
    }

    private void loadBackdrop() {
        final ImageView imageView = (ImageView) findViewById(R.id.backdrop);
        Glide.with(this).load(R.drawable.about_backdrop).centerCrop().into(imageView);
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return getString(R.string.application_title_full) + " " + pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.exception(this, e);
        }
        return "";
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.about_redsolution:
                sendUrlViewIntent(getString(R.string.about_redsolution_url));
                break;

            case R.id.about_github:
                sendUrlViewIntent(getString(R.string.about_xabber_github_url));
                break;

            case R.id.about_twitter:
                sendUrlViewIntent(getString(R.string.about_xabber_twitter_url));
                break;

            case R.id.about_text_xmpp_protocol:
                Toast.makeText(this, R.string.about_shameless_quote_from_wiki, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void sendUrlViewIntent(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
}
