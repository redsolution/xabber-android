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


import org.xmlpull.v1.XmlPullParser;

/**
 * Provides common interface to parse extensions.
 *
 * @param <Extension>
 * @author alexander.ivanov
 */
public abstract class AbstractExtensionProvider<Extension extends PacketExtension>
        extends org.jivesoftware.smack.provider.ExtensionElementProvider<Extension>
{

    /**
     * Creates an instance.
     * <p/>
     * Parser position mustn't be changed.
     *
     * @param parser
     * @return
     */
    abstract protected Extension createInstance(XmlPullParser parser);

    /**
     * Parse XML tag and create instance.
     *
     * @param parser an XML parser.
     * @return new instance.
     * @throws Exception if an error occurs while parsing.
     */
    public Extension provideInstance(XmlPullParser parser) throws Exception {
        Extension instance = createInstance(parser);
        return parseTag(parser, instance);
    }

    /**
     * Parse XML tag and populate element instance. At the beginning of the
     * method call, the XML parser will be positioned on the opening tag. At the
     * end of the method call, the parser <b>must</b> be positioned on the end
     * of processed tag.
     *
     * @param parser   an XML parser.
     * @param instance instance to be updated.
     * @return updated or replaced instance.
     * @throws Exception if an error occurs parsing the XML.
     */
    public Extension parseTag(XmlPullParser parser, Extension instance) throws Exception {
        String name = parser.getName();
        instance = preProcess(parser, instance);
        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (!parseInner(parser, instance))
                    ProviderUtils.skipTag(parser);
            } else if (eventType == XmlPullParser.END_TAG) {
                if (name.equals(parser.getName()))
                    break;
                else
                    throw new IllegalStateException();
            } else if (eventType == XmlPullParser.END_DOCUMENT)
                break;
        }
        return postProcess(instance);
    }


    /**
     * Updates instance from tag name or attributes.
     * <p/>
     * Parser position mustn't be changed.
     *
     * @param instance
     * @param parser
     * @return modified instance.
     */
    protected Extension preProcess(XmlPullParser parser, Extension instance) {
        return instance;
    }

    /**
     * Called when packet have been fully parsed.
     * <p/>
     * Parser position mustn't be changed.
     *
     * @param instance
     * @return modified instance.
     */
    protected Extension postProcess(Extension instance) {
        return instance;
    }

    /**
     * Parses inner tag.
     * <p/>
     * Parser position either <b>must</b> be move to the end of processed tag,
     * either <b>mustn't</b> be changed at all.
     *
     * @param parser
     * @param instance
     * @return Whether parser position have been changed.
     * @throws Exception
     */
    protected boolean parseInner(XmlPullParser parser, Extension instance) throws Exception {
        return false;
    }


    @Override
    public Extension parse(XmlPullParser parser, int initialDepth) throws Exception {
        return provideInstance(parser);
    }

}
