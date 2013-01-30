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

import org.jivesoftware.smack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;

import com.xabber.xmpp.AbstractProvider;
import com.xabber.xmpp.Instance;
import com.xabber.xmpp.OverflowReceiverBufferException;
import com.xabber.xmpp.ProviderUtils;

abstract class AbstractDataProvider<T extends Instance, Inner extends DataHolder<T>>
		extends AbstractProvider<Inner> {

	@Override
	protected boolean parseInner(XmlPullParser parser, Inner instance)
			throws Exception {
		if (super.parseInner(parser, instance))
			return true;
		if (instance.getPayload() == null)
			if (!createPayload(parser, instance))
				return false;
		return inflatePayload(parser, instance);
	}

	protected boolean createPayload(XmlPullParser parser, Inner instance)
			throws Exception {
		if (AbstractBinaryData.TYPE_NAME.equals(parser.getName())
				|| AbstractBinaryData.BINVAL_NAME.equals(parser.getName()))
			instance.setPayload(createBinaryData());
		else if (AbstractExternalData.EXTVAL_NAME.equals(parser.getName()))
			instance.setPayload(createExternalData());
		else
			return false;
		return true;
	}

	protected abstract T createBinaryData();

	protected abstract T createExternalData();

	protected boolean inflatePayload(XmlPullParser parser, Inner instance)
			throws Exception {
		if (instance.getPayload() instanceof AbstractBinaryData)
			return inflateBinaryData(parser,
					(AbstractBinaryData) instance.getPayload());
		else if (instance.getPayload() instanceof AbstractExternalData)
			return inflateExternalData(parser,
					(AbstractExternalData) instance.getPayload());
		else
			return false;
	}

	protected boolean inflateBinaryData(XmlPullParser parser,
			AbstractBinaryData payload) throws Exception {
		if (AbstractBinaryData.TYPE_NAME.equals(parser.getName()))
			payload.setType(ProviderUtils.parseText(parser));
		else if (AbstractBinaryData.BINVAL_NAME.equals(parser.getName())) {
			String value;
			try {
				value = ProviderUtils.parseText(parser,
						AbstractBinaryData.MAX_ENCODED_DATA_SIZE);
			} catch (OverflowReceiverBufferException e) {
				return true;
			}
			payload.setData(StringUtils.decodeBase64(value));
		} else
			return false;
		return true;
	}

	protected boolean inflateExternalData(XmlPullParser parser,
			AbstractExternalData payload) throws Exception {
		if (AbstractExternalData.EXTVAL_NAME.equals(parser.getName()))
			payload.setValue(ProviderUtils.parseText(parser));
		else
			return false;
		return true;
	}

}