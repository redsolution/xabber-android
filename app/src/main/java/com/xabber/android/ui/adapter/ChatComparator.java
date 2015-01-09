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

import java.util.Comparator;

import com.xabber.android.data.message.AbstractChat;

public class ChatComparator implements Comparator<AbstractChat> {

	public static final ChatComparator CHAT_COMPARATOR = new ChatComparator();

	@Override
	public int compare(AbstractChat object1, AbstractChat object2) {
		if (object1.getLastTime() == null) {
			if (object2.getLastTime() != null)
				return 1;
			return 0;
		} else {
			if (object2.getLastTime() == null)
				return -1;
			return -object1.getLastTime().compareTo(object2.getLastTime());
		}
	}

}