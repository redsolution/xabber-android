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
        try {
            AccountJid accountJid = AccountJid.from("user@xabber.com/something");
            UserJid userJid = UserJid.from("crowdfunding@xabber.com");
            CrowdfundingChat chat = new CrowdfundingChat(accountJid, userJid, false,
                    message, unreadCount);
            return chat;
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (UserJid.UserJidCreateException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Date getLastTime() {
        return new Date((long) lastMessage.getReceivedTimestamp() * 1000);
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


}
