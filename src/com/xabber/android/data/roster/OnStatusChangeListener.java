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
package com.xabber.android.data.roster;

import com.xabber.android.data.BaseManagerInterface;
import com.xabber.android.data.account.StatusMode;

/**
 * Listen for changes in presence.
 * 
 * @author alexander.ivanov
 * 
 */
public interface OnStatusChangeListener extends BaseManagerInterface {

	/**
	 * Notify when only status text changed.
	 * 
	 * @param account
	 * @param bareAddress
	 * @param resource
	 * @param statusText
	 */
	void onStatusChanged(String account, String bareAddress, String resource,
			String statusText);

	/**
	 * Notify when status mode changed.
	 * 
	 * @param account
	 * @param bareAddress
	 * @param resource
	 * @param statusMode
	 * @param statusText
	 */
	void onStatusChanged(String account, String bareAddress, String resource,
			StatusMode statusMode, String statusText);

}
