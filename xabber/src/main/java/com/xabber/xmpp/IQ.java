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

import com.xabber.android.data.LogManager;

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

/**
 * IQ packet.
 * <p/>
 * Note: we are going to remove underlying smack package.
 *
 * @author alexander.ivanov
 */
public abstract class IQ extends org.jivesoftware.smack.packet.IQ implements Container {

    public IQ(org.jivesoftware.smack.packet.IQ iq) {
        super(iq);
    }

    protected IQ(String childElementName) {
        super(childElementName);
    }

    protected IQ(String childElementName, String childElementNamespace) {
        super(childElementName, childElementNamespace);
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
//        LogManager.i(this, "serialize ");
        SerializerUtils.serialize(serializer, this);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        String s = SerializerUtils.toXml(this);
        LogManager.i("IQ", s);

        String emptyElement = "<" + getElementName() + " xmlns=\"" + getNamespace() + "\" />";

        if (!s.contains(emptyElement)) {
            s = s.replace("<" + getElementName(), "");
            s = s.replace("xmlns=\"" + getNamespace() + "\"", "");
            s = s.replace("</" + getElementName() + ">", "");
        } else {
            s = s.replace(emptyElement, ">");
        }

        LogManager.i("IQ replaced", s);

        xml.append(s);
        return xml;
    }

}
