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

import com.xabber.xmpp.ProviderUtils;

class SoundHolderProvider extends
		AbstractDataProvider<Sound, DataHolder<Sound>> {

	@Override
	protected boolean createPayload(XmlPullParser parser,
			DataHolder<Sound> instance) throws Exception {
		if (super.createPayload(parser, instance))
			return true;
		if (PhoneticSound.PHONETIC_NAME.equals(parser.getName()))
			instance.setPayload(createPhoneticSound());
		else
			return false;
		return true;
	}

	@Override
	protected Sound createBinaryData() {
		return new BinarySound();
	}

	@Override
	protected Sound createExternalData() {
		return new ExternalSound();
	}

	protected Sound createPhoneticSound() {
		return new PhoneticSound();
	}

	@Override
	protected boolean inflatePayload(XmlPullParser parser,
			DataHolder<Sound> instance) throws Exception {
		if (super.inflatePayload(parser, instance))
			return true;
		if (instance.getPayload() instanceof PhoneticSound)
			return inflatePhoneticSound(parser,
					(PhoneticSound) instance.getPayload());
		else
			return false;
	}

	protected boolean inflatePhoneticSound(XmlPullParser parser,
			PhoneticSound payload) throws Exception {
		if (PhoneticSound.PHONETIC_NAME.equals(parser.getName()))
			payload.setValue(ProviderUtils.parseText(parser));
		else
			return false;
		return true;
	}

	@Override
	protected DataHolder<Sound> createInstance(XmlPullParser parser) {
		return new DataHolder<Sound>();
	}

	private SoundHolderProvider() {
	}

	private static final SoundHolderProvider instance = new SoundHolderProvider();

	public static SoundHolderProvider getInstance() {
		return instance;
	}

}