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
package com.xabber.android.ui.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.account.AccountErrorEvent;
import com.xabber.android.data.xaccount.XabberAccountManager;
import com.xabber.android.ui.dialog.AccountEnterPassDialog;
import com.xabber.android.ui.dialog.AccountErrorDialogFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.xabber.android.data.account.AccountErrorEvent.Type.CONNECTION;

/**
 * Base class for all Activities.
 * <p/>
 * Adds custom activity logic.
 *
 * @author alexander.ivanov
 */
public abstract class ManagedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ActivityManager.getInstance().onCreate(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        EventBus.getDefault().register(this);
        ActivityManager.getInstance().onResume(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        ActivityManager.getInstance().onPause(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ActivityManager.getInstance().onDestroy(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        ActivityManager.getInstance().onNewIntent(this, intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ActivityManager.getInstance().onActivityResult(this, requestCode,
                resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void startActivity(Intent intent) {
        ActivityManager.getInstance().updateIntent(this, intent);
        super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        ActivityManager.getInstance().updateIntent(this, intent);
        super.startActivityForResult(intent, requestCode);
    }

    @Subscribe(sticky = true)
    public void onAuthErrorEvent(AccountErrorEvent accountErrorEvent) {
        if (!accountErrorEvent.getType().equals(CONNECTION)) {
            // show enter pass dialog
            AccountEnterPassDialog.newInstance(accountErrorEvent)
                    .show(getFragmentManager(), AccountEnterPassDialog.class.getSimpleName());
        } else {
            // show error dialog
            AccountErrorDialogFragment.newInstance(accountErrorEvent)
                    .show(getFragmentManager(), AccountErrorDialogFragment.class.getSimpleName());
        }
        EventBus.getDefault().removeStickyEvent(accountErrorEvent);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onXabberAccountDeleted(XabberAccountManager.XabberAccountDeletedEvent event) {
        showAlert(getString(R.string.account_deleted));
    }

    public void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EventBus.getDefault().removeStickyEvent(XabberAccountManager.XabberAccountDeletedEvent.class);
                    }
                });
        Dialog dialog = builder.create();
        dialog.show();
    }

}
