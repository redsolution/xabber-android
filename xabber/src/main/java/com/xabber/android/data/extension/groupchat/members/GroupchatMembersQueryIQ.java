package com.xabber.android.data.extension.groupchat.members;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ;

public class GroupchatMembersQueryIQ extends GroupchatAbstractQueryIQ {
    public static final String QUERY_TYPE = "#members";
    public static final String VERSION = "version";
    public static final String MEMBER_ID = "id";

    private String queryId;
    private String queryVersion;

    public GroupchatMembersQueryIQ() {
        super(ELEMENT, NAMESPACE + QUERY_TYPE);
        setType(Type.get);
    }

    public GroupchatMembersQueryIQ(ContactJid groupchatJid) {
        super(ELEMENT, NAMESPACE + QUERY_TYPE);
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

    @Override
    public RequestType getRequestType() {
        return RequestType.MemberList;
    }
}
