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

import com.xabber.android.data.roster.AbstractContact;

public class ComparatorByName implements Comparator<AbstractContact> {

	public static final ComparatorByName COMPARATOR_BY_NAME = new ComparatorByName();

	@Override
	public int compare(AbstractContact object1, AbstractContact object2) {
		int result;
		result = object1.getName().compareToIgnoreCase(object2.getName());
		if (result != 0)
			return result;
		return object1.getAccount().compareToIgnoreCase(object2.getAccount());
	}

}
