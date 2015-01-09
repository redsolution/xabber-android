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
package com.xabber.xmpp.time;

import java.io.IOException;
import java.util.Date;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.IQ;
import com.xabber.xmpp.SerializerUtils;

/**
 * Entity time packet.
 * 
 * http://xmpp.org/extensions/xep-0202.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Time extends IQ {

	public static final String ELEMENT_NAME = "time";
	public static final String NAMESPACE = "urn:xmpp:time";

	public static final String TZO_NAME = "tzo";
	public static final String UTC_NAME = "utc";

	/**
	 * Time zone offset in minutes.
	 */
	private Integer tzo;
	private Date utc;
	private final Date created;

	public Time() {
		created = new Date();
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if (tzo != null) {
			String value;
			if (tzo == 0) {
				value = "z";
			} else {
				boolean positive = tzo > 0;
				int abs = Math.abs(tzo);
				value = String.format("%s%02d:%02d", positive ? "+" : "-",
						(abs / 60) % 24, abs % 60);
			}
			SerializerUtils.addTextTag(serializer, TZO_NAME, value);
		}
		if (utc != null)
			SerializerUtils.addDateTimeTag(serializer, UTC_NAME, utc);
	}

	@Override
	public boolean isValid() {
		return tzo != null && utc != null;
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	/**
	 * @return Time zone offset in minutes.
	 */
	public Integer getTzo() {
		return tzo;
	}

	/**
	 * @param tzo
	 *            Time zone offset in minutes.
	 */
	public void setTzo(Integer tzo) {
		this.tzo = tzo;
	}

	public Date getUtc() {
		return utc;
	}

	public void setUtc(Date utc) {
		this.utc = utc;
	}

	/**
	 * @return Time when object has been created.
	 */
	public Date getCreated() {
		return created;
	}

}
