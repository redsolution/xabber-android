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

import com.xabber.android.data.roster.AbstractContact;

public class ComparatorByStatus extends ComparatorByName {

	public static final ComparatorByStatus COMPARATOR_BY_STATUS = new ComparatorByStatus();

	@Override
	public int compare(AbstractContact object1, AbstractContact object2) {
		int result;
		result = object1.getStatusMode().compareTo(object2.getStatusMode());
		if (result != 0)
			return result;
		return super.compare(object1, object2);
	}

}
