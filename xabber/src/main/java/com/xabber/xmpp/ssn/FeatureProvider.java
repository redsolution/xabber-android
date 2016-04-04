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

import com.xabber.xmpp.AbstractExtensionProvider;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.xmlpull.v1.XmlPullParser;

public class FeatureProvider extends AbstractExtensionProvider<Feature> {

    @Override
    protected Feature createInstance(XmlPullParser parser) {
        return new Feature();
    }

    @Override
    protected boolean parseInner(XmlPullParser parser, Feature instance) throws Exception {
        if (super.parseInner(parser, instance))
            return true;
        if (DataForm.ELEMENT.equals(parser.getName())
                && DataForm.NAMESPACE.equals(parser.getNamespace())) {
            ExtensionElement packetExtension = PacketParserUtils.
                    parseExtensionElement(DataForm.ELEMENT, DataForm.NAMESPACE, parser);
            if (packetExtension instanceof DataForm)
                instance.setDataForm((DataForm) packetExtension);
            return true;
        }
        return false;
    }

}
