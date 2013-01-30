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
package com.xabber.xmpp.ssn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.FormField.Option;
import org.jivesoftware.smackx.packet.DataForm;
import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.PacketExtension;
import com.xabber.xmpp.ProviderUtils;
import com.xabber.xmpp.form.DataFormType;

/**
 * Packet extension for Stanza Session Negotiation.
 * 
 * http://xmpp.org/extensions/xep-0155.html
 * 
 * @author alexander.ivanov
 * 
 */
public class Feature extends PacketExtension {

	private static final String NAMESPACE = "http://jabber.org/protocol/feature-neg";
	private static final String ELEMENT_NAME = "feature";

	public static final String FORM_TYPE_FIELD = "FORM_TYPE";
	public static final String FORM_TYPE_VALUE = "urn:xmpp:ssn";
	private static final String ACCEPT_FIELD = "accept";
	private static final String RENEGOTIATE_FIELD = "renegotiate";
	private static final String TERMINATE_FIELD = "terminate";
	public static final String LOGGING_FIELD = "logging";
	private static final String DISCLOSURE_FIELD = "disclosure";
	private static final String SECURITY_FIELD = "security";

	private DataForm dataForm;

	public Feature() {
	}

	public Feature(DataForm dataForm) {
		this();
		setDataForm(dataForm);
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		dataForm.serialize(serializer);
	}

	@Override
	public boolean isValid() {
		if (dataForm == null)
			return false;
		DataFormType dataFormType = getDataFormType();
		if (dataFormType == null || dataFormType == DataFormType.cancel)
			return false;
		int selected = 0;
		if (getAcceptValue() != null)
			selected += 1;
		if (getRenegotiateValue() != null)
			selected += 1;
		if (getTerminateValue() != null) {
			if (!getTerminateValue()
					|| getDataFormType() != DataFormType.submit)
				return false;
			selected += 1;
		}
		if (selected != 1)
			return false;
		return FORM_TYPE_VALUE.equals(getValue(FORM_TYPE_FIELD));
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	public DataForm getDataForm() {
		return dataForm;
	}

	public void setDataForm(DataForm dataForm) {
		this.dataForm = dataForm;
	}

	static public DataForm createDataForm(DataFormType type) {
		DataForm dataForm = new DataForm(type.toString());
		FormField typeField = new FormField(FORM_TYPE_FIELD);
		typeField.addValue(FORM_TYPE_VALUE);
		typeField.setType(FormField.TYPE_HIDDEN);
		dataForm.addField(typeField);
		return dataForm;
	}

	static private void addRequiredBooleanField(DataForm dataForm, String name,
			String label, boolean value) {
		FormField field = new FormField(name);
		field.setRequired(true);
		field.setLabel(label);
		field.addValue(Boolean.valueOf(value).toString());
		field.setType(FormField.TYPE_BOOLEAN);
		dataForm.addField(field);
	}

	static public void addAcceptField(DataForm dataForm, boolean value) {
		addRequiredBooleanField(dataForm, ACCEPT_FIELD, "Accept this session?",
				value);
	}

	static public void addRenegotiateField(DataForm dataForm, boolean value) {
		addRequiredBooleanField(dataForm, RENEGOTIATE_FIELD, "Renegotiate?",
				value);
	}

	static public void addTerminateField(DataForm dataForm) {
		addRequiredBooleanField(dataForm, TERMINATE_FIELD, null, true);
	}

	static public void addLoggingField(DataForm dataForm,
			LoggingValue[] options, LoggingValue value) {
		FormField field = new FormField(LOGGING_FIELD);
		field.setRequired(true);
		field.setLabel("Message logging");
		field.setType(FormField.TYPE_LIST_SINGLE);
		if (options != null)
			for (LoggingValue loggingValue : options)
				field.addOption(loggingValue.createOption());
		field.addValue(value.name());
		dataForm.addField(field);
	}

	public static void addDisclosureField(DataForm dataForm,
			DisclosureValue[] options, DisclosureValue value) {
		FormField field = new FormField(DISCLOSURE_FIELD);
		field.setRequired(false);
		field.setLabel("Disclosure of content, decryption keys or identities");
		field.setType(FormField.TYPE_LIST_SINGLE);
		if (options != null)
			for (DisclosureValue loggingValue : options)
				field.addOption(loggingValue.createOption());
		field.addValue(value.name());
		dataForm.addField(field);
	}

	public static void addSecurityField(DataForm dataForm,
			SecurityValue[] options, SecurityValue value) {
		FormField field = new FormField(SECURITY_FIELD);
		field.setRequired(false);
		field.setLabel("Minimum security level");
		field.setType(FormField.TYPE_LIST_SINGLE);
		if (options != null)
			for (SecurityValue loggingValue : options)
				field.addOption(loggingValue.createOption());
		field.addValue(value.name());
		dataForm.addField(field);
	}

	private FormField getField(String name) {
		for (Iterator<FormField> i = dataForm.getFields(); i.hasNext();) {
			FormField formField = i.next();
			if (name.equals(formField.getVariable()))
				return formField;
		}
		return null;
	}

	public Collection<LoggingValue> getLoggingOptions() {
		FormField field = getField(LOGGING_FIELD);
		if (field == null)
			return null;
		Collection<LoggingValue> collection = new ArrayList<LoggingValue>();
		Iterator<Option> iterator = field.getOptions();
		while (iterator.hasNext())
			try {
				collection.add(LoggingValue.fromString(iterator.next()
						.getValue()));
			} catch (NoSuchElementException e) {
				continue;
			}
		return collection;
	}

	public Collection<DisclosureValue> getDisclosureOptions() {
		FormField field = getField(DISCLOSURE_FIELD);
		if (field == null)
			return null;
		Collection<DisclosureValue> collection = new ArrayList<DisclosureValue>();
		Iterator<Option> iterator = field.getOptions();
		while (iterator.hasNext())
			try {
				collection.add(DisclosureValue.fromString(iterator.next()
						.getValue()));
			} catch (NoSuchElementException e) {
				continue;
			}
		return collection;
	}

	public Collection<SecurityValue> getSecurityOptions() {
		FormField field = getField(SECURITY_FIELD);
		if (field == null)
			return null;
		Collection<SecurityValue> collection = new ArrayList<SecurityValue>();
		Iterator<Option> iterator = field.getOptions();
		while (iterator.hasNext())
			try {
				collection.add(SecurityValue.fromString(iterator.next()
						.getValue()));
			} catch (NoSuchElementException e) {
				continue;
			}
		return collection;
	}

	private String getValue(String name) {
		FormField field = getField(name);
		if (field == null)
			return null;
		Iterator<String> iterator = field.getValues();
		if (iterator.hasNext())
			return iterator.next();
		return null;
	}

	public LoggingValue getLoggingValue() {
		String value = getValue(LOGGING_FIELD);
		try {
			return LoggingValue.fromString(value);
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	public Boolean getAcceptValue() {
		return ProviderUtils.parseBoolean(getValue(ACCEPT_FIELD));
	}

	public Boolean getRenegotiateValue() {
		return ProviderUtils.parseBoolean(getValue(RENEGOTIATE_FIELD));
	}

	public Boolean getTerminateValue() {
		return ProviderUtils.parseBoolean(getValue(TERMINATE_FIELD));
	}

	public DataFormType getDataFormType() {
		try {
			return DataFormType.fromString(dataForm.getType());
		} catch (NoSuchElementException e) {
			return null;
		}
	}

}
