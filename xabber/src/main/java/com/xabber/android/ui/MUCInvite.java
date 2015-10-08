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
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.extension.muc.RoomInvite;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.ui.helper.ManagedDialog;

public class MUCInvite extends ManagedDialog {

    private String account;
    private String room;
    private RoomInvite roomInvite;

    public static Intent createIntent(Context context, String account,
                                      String room) {
        return new EntityIntentBuilder(context, MUCInvite.class)
                .setAccount(account).setUser(room).build();
    }

    private static String getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static String getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        account = getAccount(intent);
        room = getUser(intent);
        roomInvite = MUCManager.getInstance().getInvite(account, room);
        if (roomInvite == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        setDialogMessage(roomInvite.getConfirmation());
        setDialogTitle(R.string.subscription_request_message);
        findViewById(android.R.id.button3).setVisibility(View.GONE);
    }

    @Override
    public void onAccept() {
        super.onAccept();
        startActivity(ConferenceAdd.createIntent(this, account, room));
        finish();
    }

    @Override
    public void onDecline() {
        super.onDecline();
        MUCManager.getInstance().removeInvite(roomInvite);
        finish();
    }

}
