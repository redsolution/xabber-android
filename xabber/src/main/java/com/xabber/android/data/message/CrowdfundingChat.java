package com.xabber.android.data.message;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.database.realm.CrowdfundingMessage;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;

import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Date;

public class CrowdfundingChat extends AbstractChat {

    public final static String ACCOUNT = "user@xabber.com/something";
    public final static String USER = "crowdfunding@xabber.com";

    private CrowdfundingMessage lastMessage;
    private int unreadCount;

    public CrowdfundingChat(@NonNull AccountJid account, @NonNull UserJid user,
                            boolean isPrivateMucChat, CrowdfundingMessage lastMessage,
                            int unreadCount) {
        super(account, user, isPrivateMucChat);
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
    }

    public static CrowdfundingChat createCrowdfundingChat(int unreadCount, CrowdfundingMessage message) {
        AccountJid accountJid = getDefaultAccount();
        UserJid userJid = getDefaultUser();
        if (accountJid != null && userJid != null) {
            CrowdfundingChat chat = new CrowdfundingChat(accountJid, userJid, false,
                    message, unreadCount);
            return chat;
        } else return null;
    }

    public Date getLastTime() {
        if (lastMessage != null)
            return new Date((long) lastMessage.getReceivedTimestamp() * 1000);
        return new Date(0);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    @Nullable
    public CrowdfundingMessage getLastCrowdMessage() {
        return lastMessage;
    }

    @Override
    protected String parseInnerMessage(boolean ui, Message message, String parentMessageId) {
        return null;
    }

    @NonNull
    @Override
    public Jid getTo() {
        return null;
    }

    @Override
    public Message.Type getType() {
        return null;
    }

    @Override
    protected MessageItem createNewMessageItem(String text) {
        return null;
    }

    @Nullable
    public static AccountJid getDefaultAccount() {
        try {
            return AccountJid.from(ACCOUNT);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public static UserJid getDefaultUser() {
        try {
            return UserJid.from(USER);
        } catch (UserJid.UserJidCreateException e) {
            e.printStackTrace();
            return null;
        }
    }
}
