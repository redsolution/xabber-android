package com.xabber.android.data.extension.groupchat.block;

import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ;

public class GroupchatBlocklistQueryIQ extends GroupchatAbstractQueryIQ {

    public static final String HASH_BLOCK = "#block";

    public GroupchatBlocklistQueryIQ(ContactJid groupchatJid) {
        super(NAMESPACE + HASH_BLOCK);
        setType(Type.get);
        setTo(groupchatJid.getBareJid());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }

}
