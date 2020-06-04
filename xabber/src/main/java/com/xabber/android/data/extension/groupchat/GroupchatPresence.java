package com.xabber.android.data.extension.groupchat;

import org.jivesoftware.smack.util.XmlStringBuilder;

public class GroupchatPresence extends GroupchatExtensionElement {

    public static final String NAME = "name";
    public static final String PRIVACY = "privacy";
    public static final String PINNED_MESSAGE = "pinned-message";
    public static final String COLLECT = "collect";
    public static final String PEER_TO_PEER = "peer-to-peer";
    public static final String STATUS = "status";
    public static final String PRESENT = "present";
    public static final String MEMBERS = "members";

    private String name;
    private String privacy;
    private String pinnedMessageId;
    private boolean collect;
    private boolean p2p;
    private String status;
    private int presentMembers;
    private int allMembers;

    public GroupchatPresence() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getPinnedMessageId() {
        return pinnedMessageId;
    }

    public void setPinnedMessageId(String pinnedMessageId) {
        this.pinnedMessageId = pinnedMessageId;
    }

    public boolean isCollect() {
        return collect;
    }

    public void setCollect(boolean collect) {
        this.collect = collect;
    }

    public boolean isP2p() {
        return p2p;
    }

    public void setP2p(boolean p2p) {
        this.p2p = p2p;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPresentMembers() {
        return presentMembers;
    }

    public void setPresentMembers(int presentMembers) {
        this.presentMembers = presentMembers;
    }

    public int getAllMembers() {
        return allMembers;
    }

    public void setAllMembers(int allMembers) {
        this.allMembers = allMembers;
    }

    @Override
    public void appendToXML(XmlStringBuilder xml) {
        xml.optElement(NAME, name);
        xml.optElement(PRIVACY, privacy);
        xml.optElement(PINNED_MESSAGE, pinnedMessageId);

        xml.optElement(COLLECT, collect ? "yes" : "no");
        xml.optElement(PEER_TO_PEER, p2p ? "true" : "false");

        xml.optElement(STATUS, status);
        xml.optElement(PRESENT, presentMembers);
        xml.optElement(MEMBERS, allMembers);
    }
}
