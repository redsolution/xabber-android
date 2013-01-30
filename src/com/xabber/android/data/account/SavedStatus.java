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
package com.xabber.android.data.account;


public class SavedStatus implements Comparable<SavedStatus> {

	private final StatusMode statusMode;
	private final String statusText;

	public SavedStatus(StatusMode statusMode, String statusText) {
		super();
		this.statusMode = statusMode;
		this.statusText = statusText;
	}

	public StatusMode getStatusMode() {
		return statusMode;
	}

	public String getStatusText() {
		return statusText;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((statusMode == null) ? 0 : statusMode.hashCode());
		result = prime * result
				+ ((statusText == null) ? 0 : statusText.hashCode());
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
		SavedStatus other = (SavedStatus) obj;
		if (statusMode != other.statusMode)
			return false;
		if (statusText == null) {
			if (other.statusText != null)
				return false;
		} else if (!statusText.equals(other.statusText))
			return false;
		return true;
	}

	@Override
	public int compareTo(SavedStatus another) {
		int result = statusMode.compareTo(another.statusMode);
		if (result != 0)
			return result;
		return statusText.compareToIgnoreCase(another.statusText);
	}

}
