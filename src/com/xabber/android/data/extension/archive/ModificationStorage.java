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
package com.xabber.android.data.extension.archive;

import java.util.Date;

public class ModificationStorage extends HeaderSequence {

	/**
	 * Whether all headers and messages has been received.
	 */
	private boolean succeed;

	/**
	 * Last received modification request.
	 */
	private Date last;

	/**
	 * Time stamp when request has been sent.
	 */
	private Date request;

	public ModificationStorage() {
		last = null;
		request = null;
		succeed = false;
	}

	public Date getLastRequest() {
		return last;
	}

	public boolean isSucceed() {
		return succeed;
	}

	/**
	 * Reset storage state and mark as in progress.
	 */
	public void onConnected() {
		super.reset();
		super.setInProgress(true);
		succeed = false;
	}

	/**
	 * Stores request time.
	 * 
	 * @param request
	 * @return Whether modification request should be performed.
	 */
	public boolean request(Date request) {
		if (last == null) {
			last = request;
			return false;
		} else {
			this.request = request;
			return true;
		}
	}

	/**
	 * All headers and messages has been received.
	 */
	public void onSuccess() {
		last = request;
		request = null;
		succeed = true;
	}

	/**
	 * Modification request station has been completed (on success or on error).
	 */
	public void onFinished() {
		super.setInProgress(false);
	}

}
