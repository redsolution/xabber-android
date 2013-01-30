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
package com.xabber.xmpp;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

/**
 * Set of functions commonly used by packet writers.
 * 
 * @author alexander.ivanov
 * 
 */
public final class SerializerUtils {

	private SerializerUtils() {
	}

	/**
	 * Returned packet as string with xml. String is ready to be written to the
	 * stream.
	 * 
	 * @param instance
	 * @return
	 */
	public static String toXml(Instance instance) {
		Writer writer = new StringWriter();
		XmlSerializer serializer = Xml.newSerializer();
		try {
			serializer.setOutput(writer);
			instance.serialize(serializer);
			serializer.flush();
		} catch (IOException e) {
			return "";
		}
		return writer.toString();
	}

	/**
	 * Serialize container using its element name, namespace and content.
	 * 
	 * @param serializer
	 * @param container
	 * @throws IOException
	 */
	public static void serialize(XmlSerializer serializer, Container container)
			throws IOException {
		serializer.setPrefix("", container.getNamespace());
		serializer.startTag(container.getNamespace(),
				container.getElementName());
		container.serializeContent(serializer);
		serializer.endTag(container.getNamespace(), container.getElementName());
	}

	/**
	 * Adds inner tag.
	 * 
	 * @param serializer
	 * @param elementName
	 * @throws IOException
	 */
	public static void addEmtpyTag(XmlSerializer serializer, String elementName)
			throws IOException {
		serializer.startTag(null, elementName);
		serializer.endTag(null, elementName);
	}

	/**
	 * Adds inner tag with text payload.
	 * 
	 * @param serializer
	 * @param elementName
	 * @param innerValue
	 * @throws IOException
	 */
	public static void addTextTag(XmlSerializer serializer, String elementName,
			String innerValue) throws IOException {
		serializer.startTag(null, elementName);
		serializer.text(innerValue);
		serializer.endTag(null, elementName);
	}

	public static void addDateTimeTag(XmlSerializer serializer,
			String elementName, Date innerValue) throws IOException {
		addTextTag(serializer, elementName, serializeDateTime(innerValue));
	}

	public static void addIntegerTag(XmlSerializer serializer,
			String elementName, Integer innerValue) throws IOException {
		addTextTag(serializer, elementName, serializeInteger(innerValue));
	}

	public static void addBooleanTag(XmlSerializer serializer,
			String elementName, Boolean innerValue) throws IOException {
		addTextTag(serializer, elementName, serializeBoolean(innerValue));
	}

	/**
	 * Sets attribute.
	 * 
	 * @param serializer
	 * @param attributeName
	 * @param value
	 * @throws IOException
	 */
	public static void setTextAttribute(XmlSerializer serializer,
			String attributeName, String value) throws IOException {
		serializer.attribute(null, attributeName, value);
	}

	public static void setDateTimeAttribute(XmlSerializer serializer,
			String attributeName, Date value) throws IOException {
		setTextAttribute(serializer, attributeName, serializeDateTime(value));
	}

	public static void setIntegerAttribute(XmlSerializer serializer,
			String attributeName, Integer value) throws IOException {
		setTextAttribute(serializer, attributeName, serializeInteger(value));
	}

	public static void setBooleanAttribute(XmlSerializer serializer,
			String attributeName, Boolean value) throws IOException {
		setTextAttribute(serializer, attributeName, serializeBoolean(value));
	}

	/**
	 * Creates string with date and time according to
	 * http://xmpp.org/extensions/xep-0082.html
	 * 
	 * @param value
	 * @return <code>null</code> if value was <code>null</code>.
	 */
	public static String serializeDateTime(Date value) {
		if (value == null)
			return null;
		return StringUtils.formatXEP0082Date(value);
	}

	/**
	 * @param value
	 * @return <code>null</code> if source value was <code>null</code>.
	 */
	public static String serializeInteger(Integer value) {
		if (value == null)
			return null;
		return value.toString();
	}

	/**
	 * @param date
	 * @return <code>null</code> if value was <code>null</code>.
	 */
	public static String serializeBoolean(Boolean value) {
		if (value == null)
			return null;
		return value.toString();
	}

}
