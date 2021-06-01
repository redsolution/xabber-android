package com.xabber.android.data.database.realmobjects;

import androidx.annotation.NonNull;

import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;


public class DiscoveryInfoRealmObject extends RealmObject {

    public static class Fields {
        public static final String NODE_VER = "nodeVer";
        public static final String DISCOVERY_INFO_XML = "discoveryInfoXml";
    }

    @PrimaryKey
    private String nodeVer;

    @Required
    private String discoveryInfoXml;

    public DiscoveryInfoRealmObject() {
    }

    public DiscoveryInfoRealmObject(@NonNull String nodeVer, @NonNull DiscoverInfo discoveryInfo) {
        this.nodeVer = nodeVer;
        this.discoveryInfoXml = discoveryInfo.toXML().toString();
    }

    public DiscoverInfo getDiscoveryInfo() {
        try {
            return PacketParserUtils.parseStanza(discoveryInfoXml);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse discovery info XML from database cache");
        }
    }
}
