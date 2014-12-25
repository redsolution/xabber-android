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
package com.xabber.android.ui.helper;

import android.widget.ImageView;

import com.xabber.android.data.Application;
import com.xabber.android.data.roster.AbstractContact;

/**
 * Helper class to update avatar's contact item.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class AbstractAvatarInflaterHelper {

	/**
	 * Update avatar image view.
	 * 
	 * @param avatar
	 * @param abstractContact
	 */
	public abstract void updateAvatar(ImageView avatar,
			AbstractContact abstractContact);

	/**
	 * @return New instance depend on whether new system contact list is
	 *         supported.
	 */
	public static AbstractAvatarInflaterHelper createAbstractContactInflaterHelper() {
		if (Application.getInstance().isContactsSupported())
			return new AvatarInflaterHelper();
		else
			return new DummyAvatarInflaterHelper();
	}

}
