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
package com.xabber.android.data.extension.capability;

import java.util.regex.Pattern;

public enum ClientSoftware {

	adium("(?iu).*Adium.*"),

	empathy("(?iu).*Telepathy.*"),

	gajim("(?iu).*Gajim.*"),

	gtalk("(?iu).*Google.*Talk.*"),

	ichat("(?iu).*imagent.*"),

	miranda("(?iu).*Miranda.*"),

	pidgin("(?iu).*Pidgin.*"),

	psi("(?iu).*Psi.*"),

	qip("(?iu).*QIP.*"),

	vip("(?iu).*Xabber.*VIP.*"),

	xabber("(?iu).*Xabber.*"),

	unknown(".*");

	private Pattern regularExpression;

	private final static Pattern GTALK_NODE = Pattern
			.compile("(?iu).*mail\\.google\\.com.*client.*");

	private ClientSoftware(String regularExpression) {
		this.regularExpression = Pattern.compile(regularExpression);
	}

	/**
	 * @param name
	 * @param node
	 * @return Client software for given identity name.
	 */
	public static ClientSoftware getByName(String name, String node) {
		if (name == null) {
			if (node != null && GTALK_NODE.matcher(node).matches())
				return gtalk;
			else
				return unknown;
		}
		for (ClientSoftware clientSoftware : values())
			if (clientSoftware.regularExpression.matcher(name).matches())
				return clientSoftware;
		throw new IllegalStateException();
	}
}
