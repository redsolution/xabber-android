package com.xabber.android.data.extension.groupchat.block;

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ;
import com.xabber.android.data.message.chat.groupchat.GroupChat;

public class GroupchatBlocklistQueryIQ extends GroupchatAbstractQueryIQ {

    public static final String HASH_BLOCK = "#block";

    public GroupchatBlocklistQueryIQ(GroupChat groupChat) {
        super(NAMESPACE + HASH_BLOCK);
        setType(Type.get);
        if (groupChat.getFullJidIfPossible() != null)
            setTo(groupChat.getFullJidIfPossible());
        else setTo(groupChat.getContactJid().getJid());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }

}
