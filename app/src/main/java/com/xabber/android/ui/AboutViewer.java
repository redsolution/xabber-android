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
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.ui.helper.ManagedActivity;

public class AboutViewer extends ManagedActivity implements View.OnClickListener {

    public static Intent createIntent(Context context) {
        return new Intent(context, AboutViewer.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_viewer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_default);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(AboutViewer.this);
            }
        });

        findViewById(R.id.about_github).setOnClickListener(this);
        findViewById(R.id.about_twitter).setOnClickListener(this);
        findViewById(R.id.about_redsolution).setOnClickListener(this);

//        ((TextView) findViewById(R.id.about_version))
//                .setText(getString(R.string.about_version, getVersionName()));
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
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
        }
    }

    private void sendUrlViewIntent(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
}
