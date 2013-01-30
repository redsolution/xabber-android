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

/**
 * vCard-based structured name.
 * 
 * @author alexander.ivanov
 * 
 */
public class StructuredName {

	private final String nickName;
	private final String formattedName;
	private final String firstName;
	private final String middleName;
	private final String lastName;
	private final String bestName;

	public StructuredName(String nickName, String formattedName,
			String firstName, String middleName, String lastName) {
		super();
		this.nickName = nickName == null ? "" : nickName;
		this.formattedName = formattedName == null ? "" : formattedName;
		this.firstName = firstName == null ? "" : firstName;
		this.middleName = middleName == null ? "" : middleName;
		this.lastName = lastName == null ? "" : lastName;
		if (!"".equals(this.nickName))
			bestName = this.nickName;
		else if (!"".equals(this.formattedName))
			bestName = this.formattedName;
		else
			bestName = "";
	}

	/**
	 * @return the nick name.
	 */
	public String getNickName() {
		return nickName;
	}

	/**
	 * @return the formatted name.
	 */
	public String getFormattedName() {
		return formattedName;
	}

	/**
	 * @return the first name.
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the middle name.
	 */
	public String getMiddleName() {
		return middleName;
	}

	/**
	 * @return the last name.
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return the nick name or formatted name.
	 */
	public String getBestName() {
		return bestName;
	}

}