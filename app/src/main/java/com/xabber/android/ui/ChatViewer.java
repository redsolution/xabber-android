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
package com.xabber.android.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.helper.StatusBarPainter;

public class ChatViewer extends ChatScrollerActivity {

    private StatusBarPainter statusBarPainter;

    public static Intent createChatViewerIntent(Context context) {
        return new EntityIntentBuilder(context, ChatViewer.class).build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getResources().getBoolean(R.bool.landscape)) {
            finish();
            return;
        }

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.chat_viewer);
        statusBarPainter = new StatusBarPainter(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        LinearLayout chatScrollIndicatorLayout = (LinearLayout) findViewById(R.id.chat_scroll_indicator);

        chatScroller.createView(viewPager, chatScrollIndicatorLayout);
        chatScroller.initChats();
    }

    @Override
    public void onStatusBarNeedPaint(String account) {
        if (account == null) {
            statusBarPainter.updateWithDefaultColor();
        } else {
            statusBarPainter.updateWithAccountName(account);
        }
    }

    @Override
    public void onClose(BaseEntity chat) {
        finish();
        if (!Intent.ACTION_SEND.equals(getIntent().getAction())) {
            ActivityManager.getInstance().clearStack(false);
            if (!ActivityManager.getInstance().hasContactList(this)) {
                startActivity(ContactList.createIntent(this));
            }
        }
    }
}
