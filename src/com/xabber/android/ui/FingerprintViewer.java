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

import java.util.Collection;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.xabber.android.data.Application;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.account.OnAccountChangedListener;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.entity.BaseEntity;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.intent.AccountIntentBuilder;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.data.roster.OnContactChangedListener;
import com.xabber.android.data.roster.RosterManager;
import com.xabber.android.ui.dialog.ConfirmDialogBuilder;
import com.xabber.android.ui.dialog.ConfirmDialogListener;
import com.xabber.android.ui.dialog.DialogBuilder;
import com.xabber.android.ui.dialog.NotificationDialogBuilder;
import com.xabber.android.ui.dialog.NotificationDialogListener;
import com.xabber.android.ui.helper.ContactTitleInflater;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;
import com.xabber.xmpp.address.Jid;

public class FingerprintViewer extends ManagedActivity implements
		OnCheckedChangeListener, OnAccountChangedListener,
		OnContactChangedListener, OnClickListener, ConfirmDialogListener,
		NotificationDialogListener {

	private static final String SAVED_REMOTE_FINGERPRINT = "com.xabber.android.ui.FingerprintViewer.SAVED_REMOTE_FINGERPRINT";
	private static final String SAVED_LOCAL_FINGERPRINT = "com.xabber.android.ui.FingerprintViewer.SAVED_LOCAL_FINGERPRINT";

	private String account;
	private String user;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing())
			return;

		setContentView(R.layout.fingerprint_viewer);
		integrator = new IntentIntegrator(this);
		Intent intent = getIntent();
		account = getAccount(intent);
		user = getUser(intent);
		if (AccountManager.getInstance().getAccount(account) == null
				|| user == null) {
			Application.getInstance().onError(R.string.ENTRY_IS_NOT_FOUND);
			finish();
			return;
		}
		if (savedInstanceState != null) {
			remoteFingerprint = savedInstanceState
					.getString(SAVED_REMOTE_FINGERPRINT);
			localFingerprint = savedInstanceState
					.getString(SAVED_LOCAL_FINGERPRINT);
		} else {
			remoteFingerprint = OTRManager.getInstance().getRemoteFingerprint(
					account, user);
			localFingerprint = OTRManager.getInstance().getLocalFingerprint(
					account);
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
	}

	@Override
	protected void onResume() {
		super.onResume();
		Application.getInstance().addUIListener(OnAccountChangedListener.class,
				this);
		Application.getInstance().addUIListener(OnContactChangedListener.class,
				this);
		update();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Application.getInstance().removeUIListener(
				OnAccountChangedListener.class, this);
		Application.getInstance().removeUIListener(
				OnContactChangedListener.class, this);
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
			if (equals)
				showDialog(R.string.action_otr_smp_verified);
			else
				showDialog(R.string.action_otr_smp_unverified);
		}
	}

	@Override
	public void onContactsChanged(Collection<BaseEntity> entities) {
		String thisBareAddress = Jid.getBareAddress(user);
		for (BaseEntity entity : entities)
			if (entity.equals(account, thisBareAddress)) {
				update();
				break;
			}
	}

	@Override
	public void onAccountsChanged(Collection<String> accounts) {
		if (accounts.contains(account))
			update();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.verified:
			if (!isUpdating)
				OTRManager.getInstance().setVerify(account, user,
						remoteFingerprint, isChecked);
			break;
		default:
			break;
		}
	}

	/**
	 * Show native dialog instead of provided by ZXing.
	 * 
	 * @param alertDialog
	 */
	private void wrapInstallDialog(AlertDialog alertDialog) {
		if (alertDialog == null)
			return;
		alertDialog.dismiss();
		showDialog(R.string.zxing_install_message);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.scan:
			wrapInstallDialog(integrator
					.initiateScan(IntentIntegrator.QR_CODE_TYPES));
			break;
		case R.id.show:
			wrapInstallDialog(integrator.shareText(localFingerprint));
			break;
		case R.id.copy:
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
					.setText(((TextView) findViewById(R.id.otr_local_fingerprint))
							.getText());
			break;
		default:
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case R.string.zxing_install_message:
			return new ConfirmDialogBuilder(this, id, this).setMessage(
					R.string.zxing_install_message).create();
		case R.string.zxing_install_fail:
			return new NotificationDialogBuilder(this, id, this).setMessage(
					R.string.zxing_install_fail).create();
		case R.string.action_otr_smp_verified:
			return new NotificationDialogBuilder(this, id, this).setMessage(
					R.string.action_otr_smp_verified).create();
		case R.string.action_otr_smp_unverified:
			return new NotificationDialogBuilder(this, id, this).setMessage(
					R.string.action_otr_smp_unverified).create();
		default:
			return super.onCreateDialog(id);
		}
	}

	@Override
	public void onAccept(DialogBuilder dialogBuilder) {
		switch (dialogBuilder.getDialogId()) {
		case R.string.zxing_install_message:
			Uri uri = Uri.parse("market://details?id="
					+ IntentIntegrator.BS_PACKAGE);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException anfe) {
				showDialog(R.string.zxing_install_fail);
				break;
			}
			break;
		default:
			break;
		}
	}

	@Override
	public void onDecline(DialogBuilder dialogBuilder) {
	}

	@Override
	public void onCancel(DialogBuilder dialogBuilder) {
	}

	private void update() {
		isUpdating = true;
		AbstractContact abstractContact = RosterManager.getInstance()
				.getBestContact(account, user);
		ContactTitleInflater.updateTitle(findViewById(R.id.title), this,
				abstractContact);
		verifiedView.setChecked(OTRManager.getInstance().isVerified(account,
				user));
		scanView.setEnabled(remoteFingerprint != null);
		verifiedView.setEnabled(remoteFingerprint != null);
		((TextView) findViewById(R.id.otr_remote_fingerprint))
				.setText(remoteFingerprint == null ? getString(R.string.unknown)
						: CertificateManager.showFingerprint(remoteFingerprint));
		showView.setEnabled(localFingerprint != null);
		copyView.setEnabled(localFingerprint != null);
		((TextView) findViewById(R.id.otr_local_fingerprint))
				.setText(localFingerprint == null ? getString(R.string.unknown)
						: CertificateManager.showFingerprint(localFingerprint));
		isUpdating = false;
	}

	public static Intent createIntent(Context context, String account,
			String user) {
		return new EntityIntentBuilder(context, FingerprintViewer.class)
				.setAccount(account).setUser(user).build();
	}

	private static String getAccount(Intent intent) {
		return AccountIntentBuilder.getAccount(intent);
	}

	private static String getUser(Intent intent) {
		return EntityIntentBuilder.getUser(intent);
	}

}
