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

import java.util.NoSuchElementException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xabber.android.data.connection.CertificateInvalidReason;
import com.xabber.android.data.connection.CertificateManager;
import com.xabber.android.data.connection.ConnectionManager;
import com.xabber.android.data.connection.PendingCertificate;
import com.xabber.android.data.intent.SegmentIntentBuilder;
import com.xabber.android.ui.helper.ManagedDialog;
import com.xabber.androiddev.R;

/**
 * Dialog to confirm invalid certificate.
 * 
 * @author alexander.ivanov
 * 
 */
public class CertificateConfirmation extends ManagedDialog {

	private static final String SAVED_SHOW_DETAILS = "com.xabber.android.ui.CertificateConfirmation.SHOW_DETAILS";

	private PendingCertificate pendingCertificate;
	private boolean showDetails;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String fingerprint = getFingerprint(getIntent());
		CertificateInvalidReason reason = getReason(getIntent());
		pendingCertificate = null;
		if (fingerprint != null && reason != null)
			pendingCertificate = CertificateManager.getInstance()
					.getPendingCertificate(fingerprint, reason);
		if (pendingCertificate == null)
			finish();
		if (savedInstanceState == null) {
			showDetails = false;
		} else {
			showDetails = savedInstanceState.getBoolean(SAVED_SHOW_DETAILS,
					false);
		}
		((Button) findViewById(android.R.id.button3))
				.setText(R.string.certificate_show_details);
		setDialogTitle(R.string.INVALID_CERTIFICATE);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SAVED_SHOW_DETAILS, showDetails);
	}

	@Override
	protected void onResume() {
		super.onResume();
		update();
	}

	private void update() {
		String reason = getString(pendingCertificate.getReason()
				.getResourceId());
		String message = getString(R.string.certificate_confirmation, reason,
				CertificateManager.showFingerprint(pendingCertificate
						.getFingerprint()));
		if (showDetails) {
			String details = getString(R.string.certificate_details,
					pendingCertificate.getIssuerCommonName(),
					pendingCertificate.getIssuerOrganization(),
					pendingCertificate.getIssuerOrganizationlUnit(),
					pendingCertificate.getSerialNumber(),
					pendingCertificate.getSubjectCommonName(),
					pendingCertificate.getSubjectOrganization(),
					pendingCertificate.getSubjectOrganizationlUnit(),
					pendingCertificate.issuedOn(),
					pendingCertificate.expiresOn());
			message += details;
			findViewById(android.R.id.button3).setVisibility(View.GONE);
		}
		((TextView) findViewById(android.R.id.message)).setText(message);
	}

	@Override
	public void onAccept() {
		super.onAccept();
		CertificateManager.getInstance().accept(
				pendingCertificate.getFingerprint(),
				pendingCertificate.getReason());
		ConnectionManager.getInstance().updateConnections(true);
		finish();
	}

	@Override
	public void onDecline() {
		super.onDecline();
		CertificateManager.getInstance().discard(
				pendingCertificate.getFingerprint(),
				pendingCertificate.getReason());
		finish();
	}

	@Override
	public void onCenter() {
		super.onCenter();
		showDetails = true;
		update();
	}

	public static Intent createIntent(Context context, String fingerPrint,
			CertificateInvalidReason reason) {
		return new SegmentIntentBuilder<SegmentIntentBuilder<?>>(context,
				CertificateConfirmation.class).addSegment(fingerPrint)
				.addSegment(reason.toString()).build();
	}

	private static String getFingerprint(Intent intent) {
		return SegmentIntentBuilder.getSegment(intent, 0);
	}

	private static CertificateInvalidReason getReason(Intent intent) {
		String value = SegmentIntentBuilder.getSegment(intent, 1);
		if (value != null)
			try {
				return CertificateInvalidReason.valueOf(value);
			} catch (NoSuchElementException e) {
			}
		return null;
	}

}
