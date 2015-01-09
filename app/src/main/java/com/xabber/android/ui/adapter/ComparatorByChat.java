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
package com.xabber.android.ui.adapter;

import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;

public class ComparatorByChat extends ComparatorByName {

	public static final ComparatorByChat COMPARATOR_BY_CHAT = new ComparatorByChat();

	@Override
	public int compare(AbstractContact object1, AbstractContact object2) {
		final MessageManager messageManager = MessageManager.getInstance();
		final AbstractChat abstractChat1 = messageManager.getChat(
				object1.getAccount(), object1.getUser());
		final AbstractChat abstractChat2 = messageManager.getChat(
				object2.getAccount(), object2.getUser());
		final boolean hasActiveChat1 = abstractChat1 != null
				&& abstractChat1.isActive();
		final boolean hasActiveChat2 = abstractChat2 != null
				&& abstractChat2.isActive();
		if (hasActiveChat1 && !hasActiveChat2)
			return -1;
		if (!hasActiveChat1 && hasActiveChat2)
			return 1;
		if (hasActiveChat1) {
			int result;
			result = ChatComparator.CHAT_COMPARATOR.compare(abstractChat1,
					abstractChat2);
			if (result != 0)
				return result;
		}
		return super.compare(object1, object2);
	}

}
