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
package com.xabber.android.ui.preferences;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.xabber.android.R;
import com.xabber.android.data.ActivityManager;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.ui.activity.AccountActivity;
import com.xabber.android.ui.activity.AccountAddActivity;
import com.xabber.android.ui.activity.AccountListActivity;
import com.xabber.android.ui.activity.ContactListActivity;
import com.xabber.android.ui.activity.ManagedActivity;
import com.xabber.android.ui.activity.StatusEditActivity;
import com.xabber.android.ui.adapter.AccountListPreferenceAdapter;
import com.xabber.android.ui.color.BarPainter;
import com.xabber.android.ui.dialog.AccountDeleteDialog;
import com.xabber.android.ui.helper.ToolbarHelper;
import com.xabber.android.ui.widget.XMPPListPreference;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import rx.subscriptions.CompositeSubscription;

public class PreferenceEditor extends ManagedActivity
        implements PreferencesFragment.OnPreferencesFragmentInteractionListener, AccountListPreferenceAdapter.Listener {

    private BarPainter barPainter;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private ProgressDialog progressDialog;
    private final static String LOG_TAG = PreferenceEditor.class.getSimpleName();

    public static Intent createIntent(Context context) {
        return new Intent(context, PreferenceEditor.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        setContentView(R.layout.activity_with_toolbar_and_container);

        Toolbar toolbar = ToolbarHelper.setUpDefaultToolbar(this);
        barPainter = new BarPainter(this, toolbar);


        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new PreferencesFragment()).commit();
        }

        // Force request sound. This will set default value if not specified.
        SettingsManager.eventsSound();
        SettingsManager.chatsAttentionSound();
    }

    @Override
    protected void onResume() {
        super.onResume();
        barPainter.setDefaultColor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeSubscription.clear();
    }

    protected void showProgress(String title) {
        if (!isFinishing()) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(title);
            progressDialog.setMessage(getResources().getString(R.string.progress_message));
            progressDialog.show();
        }
    }

    protected void hideProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            LogManager.exception(this, e);
        }
        return "";
    }

    @Override
    public void onThemeChanged() {
        ActivityManager.getInstance().clearStack(true);
        startActivity(ContactListActivity.createIntent(this));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddAccountClick(XMPPListPreference.AddAccountClickEvent event) {
        startActivity(AccountAddActivity.createIntent(this));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReorderClick(XMPPListPreference.ReorderClickEvent event) {
        startActivity(AccountListActivity.createIntent(this));
    }

    @Override
    public void onAccountClick(AccountJid account) {
        startActivity(AccountActivity.createIntent(this, account));
    }

    @Override
    public void onEditAccountStatus(AccountItem accountItem) {
        startActivity(StatusEditActivity.createIntent(this, accountItem.getAccount()));
    }

    @Override
    public void onEditAccount(AccountItem accountItem) {
        startActivity(AccountActivity.createIntent(this, accountItem.getAccount()));
    }

    @Override
    public void onDeleteAccount(AccountItem accountItem) {
        AccountDeleteDialog.newInstance(accountItem.getAccount()).show(getSupportFragmentManager(),
                AccountDeleteDialog.class.getName());
    }
}
