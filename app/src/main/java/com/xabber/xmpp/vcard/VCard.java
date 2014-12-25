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
package com.xabber.xmpp.vcard;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.IQ;
import com.xabber.xmpp.SerializerUtils;

/**
 * vCard-temp
 * 
 * http://xmpp.org/extensions/xep-0054.html
 * 
 * @author alexander.ivanov
 * 
 */
public class VCard extends IQ {

	public static final String ELEMENT_NAME = "vCard";
	public static final String NAMESPACE = "vcard-temp";

	public static final String N_NAME = "N";
	public static final String CLASS_NAME = "CLASS";
	public static final String CATEGORIES_NAME = "CATEGORIES";
	public static final String KEYWORD_NAME = "KEYWORD";

	private String version;
	private final Map<NameProperty, String> name;
	private final Map<VCardProperty, String> properties;
	private final List<Photo> photos;
	private final List<Address> addresses;
	private final List<Label> labels;
	private final List<Telephone> telephones;
	private final List<Email> emails;
	private final List<Logo> logos;
	private final List<Sound> sounds;
	private final List<Geo> geos;
	private final List<Organization> organizations;
	private final List<String> categories;
	private Classification classification;
	private final List<Key> keys;

	// TODO: Agent

	public VCard() {
		name = new HashMap<NameProperty, String>();
		properties = new HashMap<VCardProperty, String>();
		photos = new ArrayList<Photo>();
		addresses = new ArrayList<Address>();
		labels = new ArrayList<Label>();
		telephones = new ArrayList<Telephone>();
		emails = new ArrayList<Email>();
		logos = new ArrayList<Logo>();
		geos = new ArrayList<Geo>();
		organizations = new ArrayList<Organization>();
		categories = new ArrayList<String>();
		sounds = new ArrayList<Sound>();
		keys = new ArrayList<Key>();
	}

	private boolean isEmpty() {
		return version == null && name.isEmpty() && properties.isEmpty()
				&& photos.isEmpty() && addresses.isEmpty() && labels.isEmpty()
				&& telephones.isEmpty() && emails.isEmpty() && logos.isEmpty()
				&& sounds.isEmpty() && geos.isEmpty() && categories.isEmpty()
				&& keys.isEmpty() && classification == null;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<Geo> getGeos() {
		return geos;
	}

	public Classification getClassification() {
		return classification;
	}

	public void setClassification(Classification classification) {
		this.classification = classification;
	}

	public List<Key> getKeys() {
		return keys;
	}

	public Map<NameProperty, String> getName() {
		return name;
	}

	public Map<VCardProperty, String> getProperties() {
		return properties;
	}

	public List<Photo> getPhotos() {
		return photos;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public List<Label> getLabels() {
		return labels;
	}

	public List<Telephone> getTelephones() {
		return telephones;
	}

	public List<Email> getEmails() {
		return emails;
	}

	public List<Logo> getLogos() {
		return logos;
	}

	public List<Organization> getOrganizations() {
		return organizations;
	}

	public List<String> getCategories() {
		return categories;
	}

	public List<Sound> getSounds() {
		return sounds;
	}

	public String getFormattedName() {
		String value = properties.get(VCardProperty.FN);
		if (value == null) {
			StringBuilder builder = new StringBuilder();
			append(builder, name.get(NameProperty.PREFIX));
			append(builder, name.get(NameProperty.GIVEN));
			append(builder, name.get(NameProperty.MIDDLE));
			append(builder, name.get(NameProperty.FAMILY));
			append(builder, name.get(NameProperty.SUFFIX));
			return builder.toString();
		}
		return value;
	}

	public byte[] getAvatar() {
		BinaryPhoto binaryPhoto = null;
		for (Photo photo : photos)
			if (photo instanceof BinaryPhoto) {
				binaryPhoto = (BinaryPhoto) photo;
				break;
			}
		if (binaryPhoto == null)
			return null;
		return binaryPhoto.getData();
	}

	public String getAvatarHash() {
		byte[] data = getAvatar();
		if (data == null)
			return null;
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		digest.update(data);
		return StringUtils.encodeHex(digest.digest());
	}

	public String getField(VCardProperty property) {
		return properties.get(property);
	}

	public String getField(NameProperty property) {
		return name.get(property);
	}

	public String getNickName() {
		return getField(VCardProperty.NICKNAME);
	}

	public String getFirstName() {
		return getField(NameProperty.GIVEN);
	}

	public String getMiddleName() {
		return getField(NameProperty.MIDDLE);
	}

	public String getLastName() {
		return getField(NameProperty.FAMILY);
	}

	private void append(StringBuilder builder, String value) {
		if (value == null || "".equals(value))
			return;
		if (builder.length() != 0)
			builder.append(" ");
		builder.append(value);
	}

	public void setFormattedName(String formattedName) {
		properties.put(VCardProperty.FN, formattedName);
	}

	@Override
	public String getElementName() {
		return ELEMENT_NAME;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

	@Override
	public void serializeContent(XmlSerializer serializer) throws IOException {
		if (isEmpty())
			return;
		SerializerUtils.addTextTag(serializer, VCardProperty.FN.toString(),
				getFormattedName());
		serializer.startTag(null, N_NAME);
		for (Entry<NameProperty, String> entry : name.entrySet())
			SerializerUtils.addTextTag(serializer, entry.getKey().toString(),
					entry.getValue());
		serializer.endTag(null, N_NAME);
		for (Entry<VCardProperty, String> entry : properties.entrySet())
			if (entry.getKey() != VCardProperty.FN)
				SerializerUtils.addTextTag(serializer, entry.getKey()
						.toString(), entry.getValue());
		for (Photo photo : photos)
			photo.serialize(serializer);
		for (Address address : addresses)
			address.serialize(serializer);
		for (Label label : labels)
			label.serialize(serializer);
		for (Telephone telephone : telephones)
			telephone.serialize(serializer);
		for (Email email : emails)
			email.serialize(serializer);
		for (Logo logo : logos)
			logo.serialize(serializer);
		for (Sound sound : sounds)
			sound.serialize(serializer);
		for (Geo geo : geos)
			geo.serialize(serializer);
		for (Organization organization : organizations)
			organization.serialize(serializer);
		if (!categories.isEmpty()) {
			serializer.startTag(null, CATEGORIES_NAME);
			for (String keyword : categories)
				SerializerUtils.addTextTag(serializer, KEYWORD_NAME, keyword);
			serializer.endTag(null, CATEGORIES_NAME);
		}
		if (classification != null)
			SerializerUtils.addTextTag(serializer, CLASS_NAME,
					classification.toString());
		for (Key key : keys)
			key.serialize(serializer);
	}

	@Override
	public boolean isValid() {
		return true;
	}

}
