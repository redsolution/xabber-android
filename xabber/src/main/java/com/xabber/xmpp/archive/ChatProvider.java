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

import com.xabber.xmpp.AbstractIQProvider;
import com.xabber.xmpp.ProviderUtils;
import com.xabber.xmpp.rsm.Set;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class ChatProvider extends AbstractIQProvider<Chat>
    //TODO
//        implements DataPacketProvider.PacketExtensionProvider
{

//    @Override
    public Chat parseExtension(XmlPullParser parser) throws Exception {
        return provideInstance(parser);
    }

    @Override
    protected Chat createInstance(XmlPullParser parser) {
        return new Chat();
    }

    @Override
    protected Chat preProcess(XmlPullParser parser, Chat instance) {
        instance.setStartString(parser.getAttributeValue(null,
                AbstractChat.START_ATTRIBUTE));
        instance.setStart(ProviderUtils.parseDateTime(parser.getAttributeValue(
                null, AbstractChat.START_ATTRIBUTE)));
        instance.setWith(parser.getAttributeValue(null,
                AbstractChat.WITH_ATTRIBUTE));
        instance.setSubject(parser.getAttributeValue(null,
                Chat.SUBJECT_ATTRIBUTE));
        instance.setThread(parser
                .getAttributeValue(null, Chat.THREAD_ATTRIBUTE));
        instance.setVersion(ProviderUtils.parseInteger(parser
                .getAttributeValue(null, Chat.VERSION_ATTRIBUTE)));
        return super.preProcess(parser, instance);
    }

    @Override
    protected boolean parseInner(XmlPullParser parser, Chat instance) throws XmlPullParserException, IOException, SmackException {
        if (super.parseInner(parser, instance))
            return true;
        String name = parser.getName();
        if (To.ELEMENT_NAME.equals(name)) {
            To value = ToProvider.getInstance().provideInstance(parser);
            if (value.isValid())
                instance.addMessage(value);
        } else if (From.ELEMENT_NAME.equals(name)) {
            From value = FromProvider.getInstance().provideInstance(parser);
            if (value.isValid())
                instance.addMessage(value);
        } else if (Next.ELEMENT_NAME.equals(name)) {
            Next value = NextProvider.getInstance().provideInstance(parser);
            if (value.isValid())
                instance.setNext(value);
        } else if (Previous.ELEMENT_NAME.equals(name)) {
            Previous value = PreviousProvider.getInstance().provideInstance(
                    parser);
            if (value.isValid())
                instance.setPrevious(value);
        } else if (Set.ELEMENT_NAME.equals(name)
                && Set.NAMESPACE.equals(parser.getNamespace())) {
            ExtensionElement packetExtension = PacketParserUtils
                    .parseExtensionElement(Set.ELEMENT_NAME, Set.NAMESPACE, parser);
            if (packetExtension instanceof Set
                    && ((Set) packetExtension).isValid())
                instance.setRsm((Set) packetExtension);
        } else
            return false;
        return true;
    }

    private static final ChatProvider instance = new ChatProvider();

    public static ChatProvider getInstance() {
        return instance;
    }

}
