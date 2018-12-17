package com.xabber.android.data.roster;

import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.message.CrowdfundingChat;

import java.util.Date;

public class CrowdfundingContact extends AbstractContact {

    private String lastMessageText;
    private Date lastMessageTime;
    private int unreadCount;

    public CrowdfundingContact(CrowdfundingChat chat) {
        super(chat.getAccount(), chat.getUser());
        CrowdfundingMessage lastMessage = chat.getLastCrowdMessage();
        if (lastMessage != null) {
            this.lastMessageText = lastMessage.getMessageForCurrentLocale();
            this.lastMessageTime = new Date((long) lastMessage.getReceivedTimestamp() * 1000);
        }
        this.unreadCount = chat.getUnreadCount();
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
