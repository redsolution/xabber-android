package com.xabber.android.data.extension.groupchat.members;

import com.xabber.android.data.message.chat.groupchat.GroupChat;

public class GroupchatMembersQueryIQ extends GroupchatAbstractMembersIQ {
    public static final String VERSION = "version";
    public static final String MEMBER_ID = "id";

    private String queryId;
    private String queryVersion;

    public GroupchatMembersQueryIQ() {
        super();
        setType(Type.get);
    }

    public GroupchatMembersQueryIQ(GroupChat groupChat) {
        super();
        setType(Type.get);
        if (groupChat.getFullJidIfPossible() != null) setTo(groupChat.getFullJidIfPossible());
        else setTo(groupChat.getContactJid().getJid());
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
