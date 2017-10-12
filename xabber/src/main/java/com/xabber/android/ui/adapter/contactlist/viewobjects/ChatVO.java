package com.xabber.android.ui.adapter.contactlist.viewobjects;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.xabber.android.R;
import com.xabber.android.data.account.AccountItem;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.connection.ConnectionState;
import com.xabber.android.data.database.messagerealm.MessageItem;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.extension.muc.MUCManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.ui.color.ColorManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created by valery.miller on 11.10.17.
 */

public class ChatVO extends ContactVO {

    private String messageText;
    private boolean isOutgoing;
    private Date time;
    private int messageStatus;

    public ChatVO(int accountColorIndicator, boolean showOfflineShadow, String name, String status,
                  int statusId, int statusLevel, Drawable avatar, int mucIndicatorLevel,
                  UserJid userJid, AccountJid accountJid, String messageText, boolean isOutgoing,
                  Date time, int messageStatus) {

        super(accountColorIndicator, showOfflineShadow, name, status, statusId, statusLevel,
                avatar, mucIndicatorLevel, userJid, accountJid);

        this.messageText = messageText;
        this.isOutgoing = isOutgoing;
        this.time = time;
        this.messageStatus = messageStatus;
    }

    public static ChatVO convert(AbstractContact contact) {

        boolean showOfflineShadow;
        int accountColorIndicator;
        Drawable avatar;
        int statusLevel;
        int mucIndicatorLevel;
        boolean isOutgoing = false;
        Date time = null;
        int messageStatus = 0;

        AccountItem accountItem = AccountManager.getInstance().getAccount(contact.getAccount());
        if (accountItem != null && accountItem.getState() == ConnectionState.connected) {
            showOfflineShadow = false;
        } else {
            showOfflineShadow = true;
        }

        accountColorIndicator = ColorManager.getInstance().getAccountPainter()
                .getAccountMainColor(contact.getAccount());
        avatar = contact.getAvatarForContactList();


        String name = contact.getName();

        if (MUCManager.getInstance().hasRoom(contact.getAccount(), contact.getUser())) {
            mucIndicatorLevel = 1;
        } else if (MUCManager.getInstance().isMucPrivateChat(contact.getAccount(), contact.getUser())) {
            mucIndicatorLevel = 2;
        } else {
            mucIndicatorLevel = 0;
        }

        statusLevel = contact.getStatusMode().getStatusLevel();
        String messageText;
        String statusText = contact.getStatusText().trim();
        int statusId = contact.getStatusMode().getStringID();

        MessageManager messageManager = MessageManager.getInstance();
        MessageItem lastMessage = messageManager.getOrCreateChat(contact.getAccount(), contact.getUser()).getLastMessage();

        if (lastMessage == null) {
            messageText = statusText;
        } else {
            if (lastMessage.getFilePath() != null) {
                messageText = new File(lastMessage.getFilePath()).getName();
            } else {
                messageText = lastMessage.getText().trim();
            }

            time = new Date(lastMessage.getTimestamp());

            isOutgoing = !lastMessage.isIncoming();

            // message status
            if (lastMessage.isForwarded()) {
                messageStatus = 1;
            } else if (lastMessage.isReceivedFromMessageArchive()) {
                messageStatus = 2;
            } else if (lastMessage.isError()) {
                messageStatus = 3;
            } else if (!lastMessage.isDelivered()) {
                if (lastMessage.isAcknowledged()) {
                    messageStatus = 4;
                } else {
                    messageStatus = 5;
                }
            }
        }

        return new ChatVO(accountColorIndicator, showOfflineShadow, name, statusText, statusId,
                statusLevel, avatar, mucIndicatorLevel, contact.getUser(), contact.getAccount(),
                messageText, isOutgoing, time, messageStatus);
    }

    public static ArrayList<ContactVO> convert(Collection<AbstractContact> contacts) {
        ArrayList<ContactVO> items = new ArrayList<>();
        for (AbstractContact contact : contacts) {
            items.add(convert(contact));
        }
        return items;
    }

    public String getMessageText() {
        return messageText;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public Date getTime() {
        return time;
    }

    public int getMessageStatus() {
        return messageStatus;
    }
}
