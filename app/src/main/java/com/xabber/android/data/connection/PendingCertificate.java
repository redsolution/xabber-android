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
package com.xabber.android.data.connection;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;

import com.xabber.android.data.Application;
import com.xabber.android.data.notification.NotificationItem;
import com.xabber.android.ui.CertificateConfirmation;
import com.xabber.android.utils.StringUtils;
import com.xabber.androiddev.R;

public class PendingCertificate implements NotificationItem {

	private static Pattern cnPattern = Pattern.compile("(?i)(cn=)([^,]*)");
	private static Pattern ouPattern = Pattern.compile("(?i)(ou=)([^,]*)");
	private static Pattern oPattern = Pattern.compile("(?i)(o=)([^,]*)");

	private final String server;

	private final X509Certificate x509Certificate;

	private final CertificateInvalidReason reason;

	private final String fingerprint;

	public PendingCertificate(String server, CertificateInvalidReason reason,
			X509Certificate x509Certificate, String fingerprint) {
		super();
		this.server = server;
		this.reason = reason;
		this.x509Certificate = x509Certificate;
		this.fingerprint = fingerprint;
	}

	public String getServer() {
		return server;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public String issuedOn() {
		return StringUtils.getDateTimeText(x509Certificate.getNotBefore());
	}

	public String expiresOn() {
		return StringUtils.getDateTimeText(x509Certificate.getNotAfter());
	}

	private String getValue(Pattern pattern, Principal principal) {
		String name = principal.getName();
		Matcher matcher = pattern.matcher(name);
		if (matcher.find())
			return matcher.group(2);
		return "";
	}

	public String getIssuerCommonName() {
		return getValue(cnPattern, x509Certificate.getSubjectDN());
	}

	public String getIssuerOrganization() {
		return getValue(oPattern, x509Certificate.getSubjectDN());
	}

	public String getIssuerOrganizationlUnit() {
		return getValue(ouPattern, x509Certificate.getSubjectDN());
	}

	public String getSubjectCommonName() {
		return getValue(cnPattern, x509Certificate.getIssuerDN());
	}

	public String getSubjectOrganization() {
		return getValue(oPattern, x509Certificate.getIssuerDN());
	}

	public String getSubjectOrganizationlUnit() {
		return getValue(ouPattern, x509Certificate.getIssuerDN());
	}

	public String getSerialNumber() {
		return x509Certificate.getSerialNumber().toString();
	}

	public CertificateInvalidReason getReason() {
		return reason;
	}

	X509Certificate getX509Certificate() {
		return x509Certificate;
	}

	@Override
	public Intent getIntent() {
		return CertificateConfirmation.createIntent(Application.getInstance(),
				getFingerprint(), reason);
	}

	@Override
	public String getTitle() {
		return Application.getInstance()
				.getString(R.string.INVALID_CERTIFICATE);
	}

	@Override
	public String getText() {
		return getServer();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fingerprint == null) ? 0 : fingerprint.hashCode());
		result = prime * result + ((reason == null) ? 0 : reason.hashCode());
		result = prime * result + ((server == null) ? 0 : server.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PendingCertificate other = (PendingCertificate) obj;
		if (fingerprint == null) {
			if (other.fingerprint != null)
				return false;
		} else if (!fingerprint.equals(other.fingerprint))
			return false;
		if (reason != other.reason)
			return false;
		if (server == null) {
			if (other.server != null)
				return false;
		} else if (!server.equals(other.server))
			return false;
		return true;
	}

}
