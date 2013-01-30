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
package com.xabber.xmpp.archive;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import com.xabber.xmpp.Instance;
import com.xabber.xmpp.SerializerUtils;

/**
 * Settings item inside the {@link Pref}.
 * 
 * @author alexander.ivanov
 * 
 */
public abstract class AbstractSettings implements Instance {

	public static final String EXPIRE_ATTRIBUTE = "expire";
	public static final String OTR_ATTRIBUTE = "otr";
	public static final String SAVE_ATTRIBUTE = "save";

	private Integer expire;
	private OtrMode otr;
	private SaveMode save;

	@Override
	public boolean isValid() {
		return true;
	}

	abstract String getElementName();

	@Override
	public void serialize(XmlSerializer serializer) throws IOException {
		serializer.startTag(null, getElementName());
		serializeAttributes(serializer);
		serializer.endTag(null, getElementName());
	}

	void serializeAttributes(XmlSerializer serializer) throws IOException {
		if (expire != null)
			SerializerUtils.setIntegerAttribute(serializer, EXPIRE_ATTRIBUTE,
					expire);
		if (otr != null)
			SerializerUtils.setTextAttribute(serializer, OTR_ATTRIBUTE,
					otr.toString());
		if (save != null)
			SerializerUtils.setTextAttribute(serializer, SAVE_ATTRIBUTE,
					save.toString());
	}

	public Integer getExpire() {
		return expire;
	}

	public void setExpire(Integer expire) {
		this.expire = expire;
	}

	public OtrMode getOtr() {
		return otr;
	}

	public void setOtr(OtrMode otr) {
		this.otr = otr;
	}

	public SaveMode getSave() {
		return save;
	}

	public void setSave(SaveMode save) {
		this.save = save;
	}

}
