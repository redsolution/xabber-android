package com.xabber.android.data.message.chat.groupchat;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.xabber.android.data.database.realmobjects.AttachmentRealmObject;
import com.xabber.android.data.database.realmobjects.ForwardIdRealmObject;
import com.xabber.android.data.database.realmobjects.MessageRealmObject;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.ContactJid;
import com.xabber.android.data.extension.groupchat.GroupchatUserExtension;
import com.xabber.android.data.extension.httpfileupload.HttpFileUploadManager;
import com.xabber.android.data.extension.otr.OTRManager;
import com.xabber.android.data.extension.otr.OTRUnencryptedException;
import com.xabber.android.data.extension.references.ReferencesManager;
import com.xabber.android.data.extension.reliablemessagedelivery.TimeElement;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.ForwardManager;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.message.MessageUtils;
import com.xabber.android.data.message.NewIncomingMessageEvent;
import com.xabber.android.data.message.chat.AbstractChat;
import com.xabber.android.data.xaccount.XMPPAuthManager;
import com.xabber.android.utils.StringUtils;
import com.xabber.xmpp.sid.UniqStanzaHelper;

import net.java.otr4j.OtrException;

import org.greenrobot.eventbus.EventBus;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;

public class GroupChat extends AbstractChat {

    private static final String LOG_TAG = GroupChat.class.getSimpleName();

    private GroupchatIndexType indexType;
    private GroupchatMembershipType membershipType;
    private GroupchatPrivacyType privacyType;

    //TODO may be Jid type
    private String owner;

    private String name;
    private String description;
    private MessageRealmObject pinnedMessage;
    private ArrayList<GroupchatMember> members;
    private String membersListVersion;
    private int numberOfMembers;
    private int numberOfOnlineMembers;

    //Permissions and restrictions
    private boolean canInvite;
    private boolean canChangeSettings;
    private boolean canChangeUsersSettings;
    private boolean canChangeNicknames;
    private boolean canChangeBadge;
    private boolean canBlockUsers;
    private boolean canChangeAvatars;


    public GroupChat(@NonNull AccountJid account, @NonNull ContactJid user) {
        super(account, user);
    }

    //todo add members list into constructor and change correspondent method at repository
    public GroupChat(@NonNull AccountJid account, @NonNull ContactJid user,
                     GroupchatIndexType indexType, GroupchatMembershipType membershipType,
                     GroupchatPrivacyType privacyType, String owner, String name,
                     String description, int numberOfMembers, ArrayList<GroupchatMember> listOfMembers,
                     MessageRealmObject pinnedMessage, String membersListVersion, boolean canInvite,
                     boolean canChangeSettings, boolean canChangeUsersSettings, boolean canChangeNicknames,
                     boolean canChangeBadge, boolean canBlockUsers, boolean canChangeAvatars) {
        super(account, user);
        this.indexType = indexType;
        this.membershipType = membershipType;
        this.privacyType = privacyType;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.numberOfMembers = numberOfMembers;
        this.members = listOfMembers;
        this.pinnedMessage = pinnedMessage;
        this.membersListVersion = membersListVersion;
        this.canInvite = canInvite;
        this.canChangeSettings = canChangeSettings;
        this.canChangeUsersSettings = canChangeUsersSettings;
        this.canChangeNicknames = canChangeNicknames;
        this.canChangeBadge = canChangeBadge;
        this.canBlockUsers = canBlockUsers;
        this.canChangeAvatars = canChangeAvatars;
    }

    @Override
    public boolean onPacket(ContactJid contactJid, Stanza packet, boolean isCarbons) {
        if (!super.onPacket(contactJid, packet, isCarbons))
            return false;
        final Resourcepart resource = packet.getFrom().getResourceOrNull();
        if (packet instanceof Presence) {
            //TODO implement presence processing

        } else if (packet instanceof Message) {
            final Message message = (Message) packet;
            if (message.getType() == Message.Type.error)
                return true;

            String text = MessageUtils.getOptimalTextBody(message);
            if (text == null)
                return true;

            DelayInformation delayInformation = message.getExtension(DelayInformation.ELEMENT, DelayInformation.NAMESPACE);
            if (delayInformation != null && "Offline Storage".equals(delayInformation.getReason())) {
                return true;
            }

            // Xabber service message received
            if (message.getType() == Message.Type.headline) {
                if (XMPPAuthManager.getInstance().isXabberServiceMessage(message.getStanzaId()))
                    return true;
            }

            String thread = message.getThread();
            updateThreadId(thread);

            boolean encrypted = OTRManager.getInstance().isEncrypted(text);

            if (!isCarbons) {
                try {
                    text = OTRManager.getInstance().transformReceiving(account, user, text);
                } catch (OtrException e) {
                    if (e.getCause() instanceof OTRUnencryptedException) {
                        text = ((OTRUnencryptedException) e.getCause()).getText();
                        encrypted = false;
                    } else {
                        LogManager.exception(this, e);
                        // Invalid message received.
                        return true;
                    }
                }
            }

            // groupchat
            String gropchatUserId = null;
            GroupchatUserExtension groupchatUser = ReferencesManager.getGroupchatUserFromReferences(packet);
            if (groupchatUser != null) {
                gropchatUserId = groupchatUser.getId();
                GroupchatMemberManager.getInstance().saveGroupchatUser(groupchatUser, contactJid.getBareJid());
            }

            RealmList<AttachmentRealmObject> attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(packet);

            String uid = UUID.randomUUID().toString();
            RealmList<ForwardIdRealmObject> forwardIdRealmObjects = parseForwardedMessage(true, packet, uid);
            String originalStanza = packet.toXML().toString();
            String originalFrom = packet.getFrom().toString();

            // forward comment (to support previous forwarded xep)
            String forwardComment = ForwardManager.parseForwardComment(packet);
            if (forwardComment != null) text = forwardComment;

            // System message received.
            if ((text == null || text.trim().equals("")) && (forwardIdRealmObjects == null || forwardIdRealmObjects.isEmpty()))
                return true;

            // modify body with references
            Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
            text = bodies.first;
            String markupText = bodies.second;
            Date timestamp = null;
            if (message.hasExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE)) {
                TimeElement timeElement = (TimeElement) message.getExtension(TimeElement.ELEMENT, TimeElement.NAMESPACE);
                timestamp = StringUtils.parseReceivedReceiptTimestampString(timeElement.getStamp());
            }
            // create message with file-attachments
            if (attachmentRealmObjects.size() > 0)
                createAndSaveFileMessage(true, uid, resource, text, markupText, null,
                        timestamp, getDelayStamp(message), true, true, encrypted,
                        MessageManager.isOfflineMessage(account.getFullJid().getDomain(), packet),
                        getStanzaId(message), UniqStanzaHelper.getOriginId(message), attachmentRealmObjects, originalStanza, null,
                        originalFrom, false, forwardIdRealmObjects, false, gropchatUserId);

                // create message without attachments
            else createAndSaveNewMessage(true, uid, resource, text, markupText, null,
                    timestamp, getDelayStamp(message), true, true, encrypted,
                    MessageManager.isOfflineMessage(account.getFullJid().getDomain(), packet),
                    getStanzaId(message), UniqStanzaHelper.getOriginId(message), originalStanza, null,
                    originalFrom, false, forwardIdRealmObjects, false, gropchatUserId);

            EventBus.getDefault().post(new NewIncomingMessageEvent(account, user));
        }
        return true;
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
                false, false, false, id, id, null, null, null,
                account.getFullJid().toString(), false, null);
    }

    @Override
    public RealmList<ForwardIdRealmObject> parseForwardedMessage(boolean ui, Stanza packet,
                                                                 String parentMessageId) {
        return super.parseForwardedMessage(ui, packet, parentMessageId);
    }

    @Override
    protected String parseInnerMessage(boolean ui, Message message, Date timestamp, String parentMessageId) {
        if (message.getType() == Message.Type.error) return null;

        final Jid fromJid = message.getFrom();
        Resourcepart resource = null;
        if (fromJid != null) resource = fromJid.getResourceOrNull();
        String text = message.getBody();
        if (text == null) return null;

        boolean encrypted = OTRManager.getInstance().isEncrypted(text);

        RealmList<AttachmentRealmObject> attachmentRealmObjects = HttpFileUploadManager.parseFileMessage(message);

        String uid = UUID.randomUUID().toString();
        RealmList<ForwardIdRealmObject> forwardIdRealmObjects = parseForwardedMessage(ui, message, uid);
        String originalStanza = message.toXML().toString();
        String originalFrom = "";
        if (fromJid != null) originalFrom = fromJid.toString();

        // groupchat
        String gropchatUserId = null;
        GroupchatUserExtension groupchatUser = ReferencesManager.getGroupchatUserFromReferences(message);
        if (groupchatUser != null) {
            gropchatUserId = groupchatUser.getId();
            GroupchatMemberManager.getInstance().saveGroupchatUser(groupchatUser, message.getFrom().asBareJid(), timestamp.getTime());
        }

        // forward comment (to support previous forwarded xep)
        String forwardComment = ForwardManager.parseForwardComment(message);
        if (forwardComment != null && !forwardComment.isEmpty()) text = forwardComment;

        // modify body with references
        Pair<String, String> bodies = ReferencesManager.modifyBodyWithReferences(message, text);
        text = bodies.first;
        String markupText = bodies.second;

        // create message with file-attachments
        if (attachmentRealmObjects.size() > 0)
            createAndSaveFileMessage(ui, uid, resource, text, markupText, null,
                    timestamp, getDelayStamp(message), true, false, encrypted,
                    false, getStanzaId(message), UniqStanzaHelper.getOriginId(message), attachmentRealmObjects,
                    originalStanza, parentMessageId, originalFrom, true, forwardIdRealmObjects, true, gropchatUserId);

            // create message without attachments
        else createAndSaveNewMessage(ui, uid, resource, text, markupText, null,
                timestamp, getDelayStamp(message), true, false, encrypted,
                false, getStanzaId(message), UniqStanzaHelper.getOriginId(message), originalStanza,
                parentMessageId, originalFrom, true, forwardIdRealmObjects, true, gropchatUserId);

        return uid;
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        sendMessages();
    }

    /* Getters and setters */

    public GroupchatIndexType getIndexType() { return indexType; }
    public void setIndexType(GroupchatIndexType indexType) { this.indexType = indexType; }

    public GroupchatMembershipType getMembershipType() { return membershipType; }
    public void setMembershipType(GroupchatMembershipType membershipType) { this.membershipType = membershipType; }

    public GroupchatPrivacyType getPrivacyType() { return privacyType; }

    public void setPrivacyType(GroupchatPrivacyType privacyType) { this.privacyType = privacyType; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MessageRealmObject getPinnedMessage() { return pinnedMessage; }
    public void setPinnedMessage(MessageRealmObject pinnedMessage) {
        this.pinnedMessage = pinnedMessage;
    }

    public ArrayList<GroupchatMember> getMembers() { return members; }
    public void setMembers(ArrayList<GroupchatMember> members) { this.members = members; }

    public String getMembersListVersion() { return membersListVersion; }
    public void setMembersListVersion(String membersListVersion) {
        this.membersListVersion = membersListVersion;
    }

    public boolean isCanInvite() { return canInvite; }
    public void setCanInvite(boolean canInvite) { this.canInvite = canInvite; }

    public boolean isCanChangeSettings() { return canChangeSettings; }
    public void setCanChangeSettings(boolean canChangeSettings) {
        this.canChangeSettings = canChangeSettings;
    }

    public boolean isCanChangeUsersSettings() { return canChangeUsersSettings; }
    public void setCanChangeUsersSettings(boolean canChangeUsersSettings) {
        this.canChangeUsersSettings = canChangeUsersSettings;
    }

    public boolean isCanChangeNicknames() { return canChangeNicknames; }
    public void setCanChangeNicknames(boolean canChangeNicknames) {
        this.canChangeNicknames = canChangeNicknames;
    }

    public boolean isCanChangeBadge() { return canChangeBadge; }
    public void setCanChangeBadge(boolean canChangeBadge) { this.canChangeBadge = canChangeBadge; }

    public boolean isCanBlockUsers() { return canBlockUsers; }
    public void setCanBlockUsers(boolean canBlockUsers) { this.canBlockUsers = canBlockUsers; }

    public boolean isCanChangeAvatars() { return canChangeAvatars; }
    public void setCanChangeAvatars(boolean canChangeAvatars) {
        this.canChangeAvatars = canChangeAvatars;
    }

    public int getNumberOfMembers() { return numberOfMembers; }
    public void setNumberOfMembers(int numberOfMembers) { this.numberOfMembers = numberOfMembers; }

    public int getNumberOfOnlineMembers() { return numberOfOnlineMembers; }
    public void setNumberOfOnlineMembers(int numberOfOnlineMembers) {
        this.numberOfOnlineMembers = numberOfOnlineMembers;
    }
}
