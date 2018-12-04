package com.xabber.android.data.roster;

import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.message.CrowdfundingChat;

public class CrowdfundingContact extends AbstractContact {

    private CrowdfundingMessage lastMessage;
    private int unreadCount;

    public CrowdfundingContact(CrowdfundingChat chat) {
        super(chat.getAccount(), chat.getUser());
        this.lastMessage = chat.getLastCrowdMessage();
        this.unreadCount = chat.getUnreadCount();
    }

    public CrowdfundingMessage getLastMessage() {
        return lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
