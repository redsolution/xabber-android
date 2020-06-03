package com.xabber.android.data.message.chat.groupchat;

import androidx.annotation.NonNull;

import com.xabber.android.data.database.realmobjects.ForwardIdRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.message.chat.AbstractChat;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.Jid;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;

public class GroupChat extends AbstractChat {

    public GroupChat(@NonNull AccountJid account, @NonNull ContactJid user) {
        super(account, user);
    }

    @NonNull
    @Override
    public Jid getTo() {
        return user.getBareJid();
    }

    @Override
    public Message.Type getType() {
        return Message.Type.groupchat;
    }

    @Override
    public MessageRealmObject createNewMessageItem(String text) {
        String id = UUID.randomUUID().toString();
        return createMessageItem(null, text, null, null, null, false,
                false, false, false, id,
                id, null, null, null,
                account.getFullJid().toString(), false, null, false);
    }

    @Override
    public RealmList<ForwardIdRealmObject> parseForwardedMessage(boolean ui, Stanza packet, String parentMessageId) {
        return super.parseForwardedMessage(ui, packet, parentMessageId);
    }

    @Override
    public String parseInnerMessage(boolean ui, Message message, Date timestamp, String parentMessageId) {
        //todo maybe like regularchat with changes
        return null;
    }

}
