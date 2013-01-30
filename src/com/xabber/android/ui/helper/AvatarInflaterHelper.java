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

import android.annotation.TargetApi;
import android.provider.ContactsContract;
import android.widget.ImageView;
import android.widget.QuickContactBadge;

import com.xabber.android.data.roster.AbstractContact;

/**
 * Helper class to add quick contact badge to the inflated contact item.
 * 
 * @author alexander.ivanov
 * 
 */
@TargetApi(5)
public class AvatarInflaterHelper extends AbstractAvatarInflaterHelper {

	@Override
	public void updateAvatar(ImageView avatar, AbstractContact abstractContact) {
		QuickContactBadge badge = (QuickContactBadge) avatar;
		badge.assignContactFromEmail(abstractContact.getUser(), true);
		badge.setMode(ContactsContract.QuickContact.MODE_SMALL);
	}

}
