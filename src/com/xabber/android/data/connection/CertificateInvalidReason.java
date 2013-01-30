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

import com.xabber.androiddev.R;

/**
 * Possible reasons for certificate invalidation.
 * 
 * @author alexander.ivanov
 * 
 */
public enum CertificateInvalidReason {

	/**
	 * Signature, subject, issuer or date verification failed.
	 */
	invalidChane,

	/**
	 * Self-signed certificate.
	 */
	selfSigned,

	/**
	 * Target verification failed.
	 */
	invalidTarget;

	public int getResourceId() {
		if (this == invalidChane)
			return R.string.certificate_invalid_chane;
		else if (this == selfSigned)
			return R.string.certificate_self_signed;
		else if (this == invalidTarget)
			return R.string.certificate_invalid_target;
		else
			throw new IllegalStateException();
	}

}
