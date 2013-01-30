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

import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractIQProvider;
import com.xabber.xmpp.ProviderUtils;

public class VCardProvider extends AbstractIQProvider<VCard> {

	@Override
	protected VCard createInstance(XmlPullParser parser) {
		return new VCard();
	}

	@Override
	protected boolean parseInner(XmlPullParser parser, VCard instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		String name = parser.getName();
		if (VCard.N_NAME.equals(name)) {
			NameInflater.getInstance().parseTag(parser, instance);
		} else if (Photo.ELEMENT_NAME.equals(name)) {
			DataHolder<Photo> holder = PhotoHolderProvider.getInstance()
					.provideInstance(parser);
			if (holder.getPayload() != null && holder.getPayload().isValid())
				instance.getPhotos().add(holder.getPayload());
		} else if (Logo.ELEMENT_NAME.equals(name)) {
			DataHolder<Logo> holder = LogoHolderProvider.getInstance()
					.provideInstance(parser);
			if (holder.getPayload() != null && holder.getPayload().isValid())
				instance.getLogos().add(holder.getPayload());
		} else if (Sound.ELEMENT_NAME.equals(name)) {
			DataHolder<Sound> holder = SoundHolderProvider.getInstance()
					.provideInstance(parser);
			if (holder.getPayload() != null && holder.getPayload().isValid())
				instance.getSounds().add(holder.getPayload());
		} else if (Address.ELEMENT_NAME.equals(name)) {
			Address value = AddressProvider.getInstance().provideInstance(
					parser);
			if (value.isValid())
				instance.getAddresses().add(value);
		} else if (Label.ELEMENT_NAME.equals(name)) {
			Label value = LabelProvider.getInstance().provideInstance(parser);
			if (value.isValid())
				instance.getLabels().add(value);
		} else if (Telephone.ELEMENT_NAME.equals(name)) {
			Telephone value = TelephoneProvider.getInstance().provideInstance(
					parser);
			if (value.isValid())
				instance.getTelephones().add(value);
		} else if (Email.ELEMENT_NAME.equals(name)) {
			Email value = EmailProvider.getInstance().provideInstance(parser);
			if (value.isValid())
				instance.getEmails().add(value);
		} else if (Geo.ELEMENT_NAME.equals(name)) {
			Geo value = GeoProvider.getInstance().provideInstance(parser);
			if (value.isValid())
				instance.getGeos().add(value);
		} else if (Organization.ELEMENT_NAME.equals(name)) {
			Organization value = OrganizationProvider.getInstance()
					.provideInstance(parser);
			if (value.isValid())
				instance.getOrganizations().add(value);
		} else if (VCard.CATEGORIES_NAME.equals(name)) {
			CategoriesInflater.getInstance().parseTag(parser, instance);
		} else if (VCard.CLASS_NAME.equals(name)) {
			ClassificationInflater.getInstance().parseTag(parser, instance);
		} else if (Key.ELEMENT_NAME.equals(name)) {
			Key value = KeyProvider.getInstance().provideInstance(parser);
			if (value.isValid())
				instance.getKeys().add(value);
		} else {
			for (VCardProperty key : VCardProperty.values())
				if (key.toString().equals(name)) {
					instance.getProperties().put(key,
							ProviderUtils.parseText(parser));
					return true;
				}
			return false;
		}
		return true;
	}

}
