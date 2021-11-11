package com.xabber.xmpp.groups.members;

import com.xabber.android.data.message.chat.GroupChat;

import org.jivesoftware.smack.packet.IQ;

public class GroupchatMembersQueryIQ extends GroupchatAbstractMembersIQ {
    public static final String VERSION = "version";
    public static final String MEMBER_ID = "id";

    private String queryId;
    private String queryVersion;

    public GroupchatMembersQueryIQ() {
        super();
        setType(IQ.Type.get);
    }

    public GroupchatMembersQueryIQ(GroupChat groupChat) {
        super();
        setType(IQ.Type.get);
        if (groupChat.getFullJidIfPossible() != null) setTo(groupChat.getFullJidIfPossible());
        else setTo(groupChat.getContactJid().getJid());
    }

    @Override
    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder xml) {
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
