/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.helper;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xabber.android.R;

/**
 * Helper for dialog activities.
 * <p/>
 * Please don't call {@link #setContentView(int)} from your
 * {@link #onCreate(Bundle)}.
 *
 * @author alexander.ivanov
 */
public abstract class ManagedDialog extends SingleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_default));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        findViewById(android.R.id.button1).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onAccept();
                    }
                });
        findViewById(android.R.id.button2).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onDecline();
                    }
                });
        findViewById(android.R.id.button3).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCenter();
                    }
                });
    }

    public void setDialogTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    public void setDialogTitle(int resid) {
        getSupportActionBar().setTitle(resid);
    }

    public void setDialogMessage(CharSequence title) {
        ((TextView) findViewById(android.R.id.message)).setText(title);
    }

    public void setDialogMessage(int resid) {
        ((TextView) findViewById(android.R.id.message)).setText(resid);
    }

    public void setCustomView(View view, boolean hideContainer) {
        ((ViewGroup) findViewById(android.R.id.custom)).addView(view);
        if (hideContainer) {
            findViewById(R.id.container).setVisibility(View.GONE);
        }
    }

    /**
     * Click on first button.
     */
    public void onAccept() {
    }

    /**
     * Click on second button.
     */
    public void onDecline() {
    }

    /**
     * Click on center button.
     */
    public void onCenter() {
    }

}
