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

import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Set of functions commonly used by packet writers.
 *
 * @author alexander.ivanov
 */
public final class SerializerUtils {

    private SerializerUtils() {
    }

    /**
     * Returned packet as string with xml. String is ready to be written to the
     * stream.
     *
     * @param instance
     * @return
     */
    public static String toXml(Instance instance) {
        Writer writer = new StringWriter();
        XmlSerializer serializer = Xml.newSerializer();
        try {
            serializer.setOutput(writer);
            instance.serialize(serializer);
            serializer.flush();
        } catch (IOException e) {
            return "";
        }
        return writer.toString();
    }

    /**
     * Serialize container using its element name, namespace and content.
     *
     * @param serializer
     * @param container
     * @throws IOException
     */
    public static void serialize(XmlSerializer serializer, Container container)
            throws IOException {
        serializer.setPrefix("", container.getNamespace());
        serializer.startTag(container.getNamespace(),
                container.getElementName());
        container.serializeContent(serializer);
        serializer.endTag(container.getNamespace(), container.getElementName());
    }

    /**
     * Adds inner tag.
     *
     * @param serializer
     * @param elementName
     * @throws IOException
     */
    public static void addEmtpyTag(XmlSerializer serializer, String elementName)
            throws IOException {
        serializer.startTag(null, elementName);
        serializer.endTag(null, elementName);
    }

    /**
     * Adds inner tag with text payload.
     *
     * @param serializer
     * @param elementName
     * @param innerValue
     * @throws IOException
     */
    public static void addTextTag(XmlSerializer serializer, String elementName,
                                  String innerValue) throws IOException {
        serializer.startTag(null, elementName);
        serializer.text(innerValue);
        serializer.endTag(null, elementName);
    }

}
