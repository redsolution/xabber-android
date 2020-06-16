package com.xabber.android.data.extension.groupchat;

import com.xabber.android.data.entity.ContactJid;

import org.jivesoftware.smack.packet.IQ;

public class GroupchatMembersQueryIQ extends IQ {

    public static final String NAMESPACE = "http://xabber.com/protocol/groupchat#members";
    public static final String ELEMENT = "query";
    public static final String VERSION = "version";
    public static final String MEMBER_ID = "id";

    private String queryId;
    private String queryVersion;

    public GroupchatMembersQueryIQ() {
        super(ELEMENT, NAMESPACE);
        setType(Type.get);
    }

    public GroupchatMembersQueryIQ(ContactJid groupchatJid) {
        super(ELEMENT, NAMESPACE);
        setType(Type.get);
        setTo(groupchatJid.getBareJid());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.optAttribute(VERSION, queryVersion);
        xml.optAttribute(MEMBER_ID, queryId);
        xml.setEmptyElement();
        return xml;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQueryVersion() {
        return queryVersion;
    }

    public void setQueryVersion(String queryVersion) {
        this.queryVersion = queryVersion;
    }
}
