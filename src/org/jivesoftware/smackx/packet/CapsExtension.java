/*
 * Copyright 2009 Jonas Ådahl.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;

public class CapsExtension implements PacketExtension {

    private String node, version, hash;
    public static final String XMLNS = "http://jabber.org/protocol/caps";
    public static final String NODE_NAME = "c";

    public CapsExtension() {
    }

    public CapsExtension(String node, String version, String hash) {
        this.node = node;
        this.version = version;
        this.hash = hash;
    }

    public String getElementName() {
        return NODE_NAME;
    }

    public String getNamespace() {
        return XMLNS;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    /*<c xmlns='http://jabber.org/protocol/caps' 
     hash='sha-1'
     node='http://code.google.com/p/exodus'
     ver='QgayPKawpkPSDYmwT/WM94uAlu0='/>
     */
    public String toXML() {
        String xml = "<c xmlns='" + XMLNS + "' " +
            "hash='" + hash + "' " +
            "node='" + node + "' " +
            "ver='" + version + "'/>";

        return xml;
    }
}
