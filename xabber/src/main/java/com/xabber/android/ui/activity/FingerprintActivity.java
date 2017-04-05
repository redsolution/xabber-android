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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.listeners.OnAccountChangedListener;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterContact;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.helper.ContactTitleActionBarInflater;

import org.jxmpp.jid.BareJid;

import java.util.Collection;

public class FingerprintActivity extends ManagedActivity implements
        OnCheckedChangeListener, OnAccountChangedListener,
        OnContactChangedListener, OnClickListener {

    private static final String SAVED_REMOTE_FINGERPRINT = "com.xabber.android.ui.activity.FingerprintViewer.SAVED_REMOTE_FINGERPRINT";
    private static final String SAVED_LOCAL_FINGERPRINT = "com.xabber.android.ui.activity.FingerprintViewer.SAVED_LOCAL_FINGERPRINT";
    ContactTitleActionBarInflater contactTitleActionBarInflater;
    private AccountJid account;
    private UserJid user;
    private String remoteFingerprint;
    private String localFingerprint;
    /**
     * UI update is in progress.
     */
    private boolean isUpdating;
    private CheckBox verifiedView;
    private View scanView;
    private View showView;
    private View copyView;
    /**
     * QR code scanner and generator.
     */
    private IntentIntegrator integrator;

    public static Intent createIntent(Context context, AccountJid account, UserJid user) {
        return new EntityIntentBuilder(context, FingerprintActivity.class)
                .setAccount(account).setUser(user).build();
    }

    private static AccountJid getAccount(Intent intent) {
        return AccountIntentBuilder.getAccount(intent);
    }

    private static UserJid getUser(Intent intent) {
        return EntityIntentBuilder.getUser(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_fingerprint);

        integrator = new IntentIntegrator(this);
        Intent intent = getIntent();
        account = getAccount(intent);
        user = getUser(intent);
        if (AccountManager.getInstance().getAccount(account) == null || user == null) {
            Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
            finish();
            return;
        }
        if (savedInstanceState != null) {
            remoteFingerprint = savedInstanceState.getString(SAVED_REMOTE_FINGERPRINT);
            localFingerprint = savedInstanceState.getString(SAVED_LOCAL_FINGERPRINT);
        } else {
            remoteFingerprint = OTRManager.getInstance().getRemoteFingerprint(account, user);
            localFingerprint = OTRManager.getInstance().getLocalFingerprint(account);
        }
        verifiedView = (CheckBox) findViewById(R.id.verified);
        verifiedView.setOnCheckedChangeListener(this);
        scanView = findViewById(R.id.scan);
        scanView.setOnClickListener(this);
        showView = findViewById(R.id.show);
        showView.setOnClickListener(this);
        copyView = findViewById(R.id.copy);
        copyView.setOnClickListener(this);
        isUpdating = false;

        contactTitleActionBarInflater = new ContactTitleActionBarInflater(this);
        contactTitleActionBarInflater.setUpActionBarView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        isUpdating = true;
        super.onRestoreInstanceState(savedInstanceState);
        isUpdating = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_REMOTE_FINGERPRINT, remoteFingerprint);
        outState.putString(SAVED_LOCAL_FINGERPRINT, localFingerprint);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult scanResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data);
        if (scanResult != null) {
            String code = scanResult.getContents();
            boolean equals = code != null && code.equals(remoteFingerprint);
            verifiedView.setChecked(equals);

            int dialogMessageId;
            if (equals) {
                dialogMessageId = R.string.action_otr_smp_verified;
            } else {
                dialogMessageId = R.string.action_otr_smp_unverified;
            }
            new AlertDialog.Builder(this).setMessage(dialogMessageId)
                    .setNeutralButton(android.R.string.ok, null).show();
        }
    }

    @Override
    public void onContactsChanged(Collection<RosterContact> entities) {
        BareJid thisBareAddress = user.getBareJid();
        for (BaseEntity entity : entities) {
            if (entity.equals(account, thisBareAddress)) {
                update();
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<AccountJid> accounts) {
        if (accounts.contains(account)) {
            update();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.verified:
                if (!isUpdating) {
                    OTRManager.getInstance().setVerify(account, user, remoteFingerprint, isChecked);
                }
                break;
            default:
                break;
        }
    }

     @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan:
                integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
                break;
            case R.id.show:
                integrator.shareText(localFingerprint);
                break;
            case R.id.copy:
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                        .setText(((TextView) findViewById(R.id.otr_local_fingerprint)).getText());
                break;
            default:
                break;
        }
    }

    private void update() {
        isUpdating = true;
        AbstractContact abstractContact = RosterManager.getInstance().getBestContact(account, user);

        verifiedView.setChecked(OTRManager.getInstance().isVerified(account, user));
        scanView.setEnabled(remoteFingerprint != null);
        verifiedView.setEnabled(remoteFingerprint != null);
        ((TextView) findViewById(R.id.otr_remote_fingerprint)).setText(
                remoteFingerprint == null ? getString(R.string.unknown) : showFingerprint(remoteFingerprint));
        showView.setEnabled(localFingerprint != null);
        copyView.setEnabled(localFingerprint != null);
        ((TextView) findViewById(R.id.otr_local_fingerprint)).setText(
                localFingerprint == null ? getString(R.string.unknown) : showFingerprint(localFingerprint));

        contactTitleActionBarInflater.update(abstractContact);

        isUpdating = false;
    }

    /**
     * @param fingerprint
     * @return Formatted fingerprint to be shown.
     */
    private static String showFingerprint(String fingerprint) {
        if (fingerprint == null)
            return null;
        StringBuffer buffer = new StringBuffer();
        for (int index = 0; index < fingerprint.length(); index++) {
            if (index > 0 && index % 2 == 0)
                buffer.append(':');
            buffer.append(fingerprint.charAt(index));
        }
        return buffer.toString().toUpperCase();
    }

}
