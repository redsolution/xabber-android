package com.xabber.android.data.extension.groupchat.block.blocklist;

import com.xabber.android.data.extension.groupchat.GroupchatAbstractQueryIQ;
import com.xabber.android.data.extension.groupchat.block.BlockGroupMemberIQ;
import com.xabber.android.data.message.chat.GroupChat;

public class GroupchatBlocklistQueryIQ extends GroupchatAbstractQueryIQ {

    public static final String HASH_BLOCK = BlockGroupMemberIQ.HASH_BLOCK;

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
